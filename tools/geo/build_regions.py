#!/usr/bin/env python3
"""TRACE 지역 경계 데이터 생성 (S08 지도 · G-01 판정).

실행:  python3 -I tools/geo/build_regions.py
의존성: Python 3.9+ 표준 라이브러리만 사용.

입력: vuski/admdongkor ver20261001 행정동 경계(GeoJSON, WGS84)
      커밋·SHA-256을 고정해 같은 결과가 다시 나오게 한다.
출력:
  app/src/main/assets/regions_display.json  화면용, 디자인 viewBox(563.3×612) 좌표, 161 지역
  app/src/main/assets/regions_lookup.json   판정용, 위경도, 법정 시·군·구(일반구 포함) 단위 + 161 지역 연결

처리:
  1) 행정동 → 시·군·구(sgg 코드) → 161 지역(디자인 PROV 규칙)으로 합친다.
     원본은 이웃 폴리곤 꼭짓점이 정확히 같으므로, 한 단위 안에서 두 번 나오는 변(내부 변)을 지우는 방식으로 합친다.
  2) 경계를 노드(세 지역 이상이 만나는 점 등)에서 끊은 '호' 단위로 한 번씩만 단순화한다.
     이웃 지역이 같은 호를 공유하므로 단순화 뒤에도 틈·겹침이 생기지 않는다.
  3) 섬이 단순화로 사라지지 않게 고리마다 최소 4점을 남기고, 면적 기준으로만 아주 작은 섬을 뺀다.

161 지역 규칙(design/html/RegionDark.dc.html의 PROV):
  - 특별시·광역시·특별자치시는 통째로 1곳. 옛 광주광역시 5개 구(전남광주통합특별시 12210~12330)도 '광주' 1곳.
  - 도의 시는 일반구를 합쳐 1곳(수원시장안구 41111 → 수원시 41110). 군은 그대로.
  - 채색 묶음(group): 서울 / 경기(+인천) / 강원 / 충북 / 충남(+대전·세종) / 전북 / 전남광주(+광주) / 경북(+대구) / 경남(+부산·울산) / 제주.
"""
import hashlib
import json
import math
import os
import sys
import subprocess
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CACHE = os.path.join(ROOT, "tools", "geo", ".cache")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets")

SRC_VERSION = "ver20261001"
SRC_COMMIT = "3bc00489b76696db864bdbea4b19985cdfa8b403"
SRC_URL = (f"https://raw.githubusercontent.com/vuski/admdongkor/{SRC_COMMIT}/"
           f"{SRC_VERSION}/HangJeongDong_{SRC_VERSION}.geojson")
SRC_SHA256 = "843ece3a0b3429742cd4a1b699cc13987a88b5e5401f382e7fdaaa194ea5e605"

# ---- 화면 배치 (디자인 RegionDark.dc.html의 지도 칸에서 읽은 자리) ----
VIEW_W, VIEW_H = 563.3, 612
# 본토 범위: 세로는 디자인 본토(15~565), 가로는 오른쪽 섬 박스(x≥449) 앞까지
MAIN_RECT = (40, 15, 445, 565)
JEJU_RECT = (462, 554, 532, 591)         # 제주 본섬이 들어가는 범위(디자인 제주시 경로 상자)
JEJU_FRAME = (449, 540, 547, 598)        # 디자인 제주 박스
ULLEUNG_RECT = (489, 52, 505, 68)        # 디자인 울릉도 모양 상자
DOKDO_RECT = (519, 78, 527.6, 82.6)      # 디자인 독도 원 두 개를 감싸는 상자
ULLEUNG_FRAME = (453, 36, 497, 82)       # 디자인 울릉 박스
LABELS = {"ulleung": ("울릉도·독도", 498, 28, "middle"), "jeju": ("제주", 460, 530, "start")}
# 본토에서 떼어 박스로 옮기는 먼 섬(읍·면 단위). 배율은 본토와 같고 위치만 옮긴다.
#   center: 박스 안 내용의 중심 자리, label: (글자, x, y, 정렬)
MOVED = [
    {"id": "baengnyeong", "sgg": "28720", "emd": ["백령면", "대청면"], "center": (30, 110),
     "label": ("백령도", 30, 146, "middle")},
    {"id": "heuksan", "sgg": "12870", "emd": ["흑산면"], "center": (36, 410),
     "label": ("흑산도", 36, 357, "middle")},
]
# 디자인 지도 위에 얹히는 UI(겹침 검사용, viewBox 좌표): 제목·원래 크기 버튼·안내 문구
UI_RECTS = {"title": (18, 16, 112, 66), "resetButton": (12, 551, 60, 597), "hint": (64, 566, 172, 588)}
LABEL_H = {"middle": 12, "start": 12}

DISPLAY_TOL = 0.2        # 단순화 허용 오차(디자인 px)
DISPLAY_MIN_AREA = 0.02  # 이보다 작은 섬(디자인 px²)은 화면용에서 뺀다. 지역마다 가장 큰 고리는 항상 남김
DISPLAY_DECIMALS = 1
LOOKUP_TOL_M = 15.0      # 판정용 단순화 허용 오차(m)
LOOKUP_MIN_AREA_M2 = 500.0
LOOKUP_DECIMALS = 5      # 약 1 m

METRO_SHORT = {"11": "서울", "26": "부산", "27": "대구", "28": "인천", "30": "대전", "31": "울산", "36": "세종"}
GWANGJU_SGG = {"12210", "12240", "12270", "12300", "12330"}
GWANGJU_CODE = "12200"  # 옛 광주광역시 묶음용 코드(원본에 없는 값, 이 앱에서만 씀)
SIDO_SHORT = {"11": "서울", "12": "전남광주", "26": "부산", "27": "대구", "28": "인천", "30": "대전", "31": "울산",
              "36": "세종", "41": "경기", "43": "충북", "44": "충남", "47": "경북", "48": "경남", "50": "제주",
              "51": "강원", "52": "전북"}
GROUPS = [  # id, 이름(범례·라벨), 포함 시·도 코드
    ("seoul", "서울", ["11"]), ("gg", "경기", ["41", "28"]), ("gw", "강원", ["51"]), ("cb", "충북", ["43"]),
    ("cn", "충남", ["44", "30", "36"]), ("jb", "전북", ["52"]), ("jn", "전남광주", ["12"]),
    ("gb", "경북", ["47", "27"]), ("gn", "경남", ["48", "26", "31"]), ("jj", "제주", ["50"]),
]
GROUP_OF_SIDO = {s: g for g, _, ss in GROUPS for s in ss}


# ------------------------------------------------------------------ 입력
def load_source():
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, f"HangJeongDong_{SRC_VERSION}.geojson")
    if not os.path.exists(path):
        print("download", SRC_URL)
        # python.org 배포판은 인증서 묶음이 없을 수 있어 curl로 받는다(무결성은 아래 SHA-256으로 확인)
        subprocess.run(["curl", "-fsSL", "-o", path + ".part", SRC_URL], check=True)
        os.replace(path + ".part", path)
    with open(path, "rb") as f:
        raw = f.read()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != SRC_SHA256:
        sys.exit(f"SHA-256 불일치: {digest} (원본이 바뀌었으면 SRC_COMMIT/SRC_SHA256을 갱신)")
    return json.loads(raw.decode("utf-8"))


def region_of(sgg, sido, sggnm):
    """sgg → (161 지역 코드, 지역 이름, 구 이름 또는 None)"""
    if sgg in GWANGJU_SGG:
        return GWANGJU_CODE, "광주", sggnm
    if sido in METRO_SHORT:
        return sido + "000", METRO_SHORT[sido], (sggnm if sido != "36" else None)
    if sggnm.endswith("구") and "시" in sggnm[:-1]:  # 도의 일반구: '수원시장안구'
        i = sggnm.index("시") + 1
        return sgg[:4] + "0", sggnm[:i], sggnm[i:]
    return sgg, sggnm, None


# ------------------------------------------------------------------ 위상(공유 변) 처리
def rings_of(geom):
    polys = geom["coordinates"] if geom["type"] == "MultiPolygon" else [geom["coordinates"]]
    for poly in polys:
        for ring in poly:
            pts = []
            for c in ring:
                p = (c[0], c[1])
                if not pts or pts[-1] != p:
                    pts.append(p)
            if len(pts) > 1 and pts[0] == pts[-1]:
                pts.pop()
            if len(pts) >= 3:
                yield pts


def ekey(a, b):
    return (a, b) if a < b else (b, a)


def dissolve(emd_rings, unit_of_emd):
    """emd_rings: [(emd_idx, ring)], unit_of_emd: emd_idx → unit.
    반환: unit → 경계 고리 목록, 노드 집합."""
    cnt = defaultdict(lambda: defaultdict(int))  # edge → unit → 횟수
    for ei, ring in emd_rings:
        u = unit_of_emd[ei]
        n = len(ring)
        for i in range(n):
            cnt[ekey(ring[i], ring[(i + 1) % n])][u] += 1
    label = {}  # 남는 변 → 그 변을 경계로 가진 단위들
    for e, d in cnt.items():
        us = tuple(sorted(u for u, c in d.items() if c % 2 == 1))
        if us:
            label[e] = us
    inc = defaultdict(list)
    for e in label:
        inc[e[0]].append(e)
        inc[e[1]].append(e)
    nodes = set()
    for v, es in inc.items():
        if len(es) != 2 or label[es[0]] != label[es[1]]:
            nodes.add(v)
    # 단위별 고리 잇기
    unit_edges = defaultdict(list)
    for e, us in label.items():
        for u in us:
            unit_edges[u].append(e)
    rings = {}
    for u, es in unit_edges.items():
        adj = defaultdict(list)
        for e in es:
            adj[e[0]].append(e)
            adj[e[1]].append(e)
        used = set()
        out = []
        for e0 in sorted(es):
            if e0 in used:
                continue
            used.add(e0)
            start, cur = e0[0], e0[1]
            ring = [start]
            while cur != start:
                ring.append(cur)
                nxt = None
                for e in adj[cur]:
                    if e not in used:
                        nxt = e
                        break
                if nxt is None:
                    break
                used.add(nxt)
                cur = nxt[1] if nxt[0] == cur else nxt[0]
            if len(ring) >= 3:
                out.append(ring)
        rings[u] = out
    return rings, nodes


# ------------------------------------------------------------------ 단순화
def dp_keep(pts, tol):
    """Douglas–Peucker. 양 끝은 항상 남김. 남길 인덱스 집합 반환."""
    n = len(pts)
    keep = {0, n - 1}
    stack = [(0, n - 1)]
    t2 = tol * tol
    while stack:
        a, b = stack.pop()
        if b <= a + 1:
            continue
        ax, ay = pts[a]
        bx, by = pts[b]
        dx, dy = bx - ax, by - ay
        L = dx * dx + dy * dy
        best, bi = -1.0, -1
        for i in range(a + 1, b):
            px, py = pts[i]
            if L == 0:
                d = (px - ax) ** 2 + (py - ay) ** 2
            else:
                t = ((px - ax) * dx + (py - ay) * dy) / L
                t = max(0.0, min(1.0, t))
                d = (px - ax - t * dx) ** 2 + (py - ay - t * dy) ** 2
            if d > best:
                best, bi = d, i
        if best > t2:
            keep.add(bi)
            stack.append((a, bi))
            stack.append((bi, b))
    return keep


def far_index(pts, a, b):
    ax, ay = pts[a]
    best, bi = -1, a
    for i in range(a, b + 1):
        d = (pts[i][0] - ax) ** 2 + (pts[i][1] - ay) ** 2
        if d > best:
            best, bi = d, i
    return bi


class Simplifier:
    """호 단위 단순화. 같은 호는 방향과 무관하게 한 번만 계산해 이웃끼리 같은 결과를 쓴다."""

    def __init__(self, proj, tol):
        self.proj, self.tol, self.cache = proj, tol, {}

    def arc(self, seq):
        fwd = tuple(seq)
        rev = fwd[::-1]
        key = min(fwd, rev)
        if key not in self.cache:
            pts = [self.proj(p) for p in key]
            keep = dp_keep(pts, self.tol)
            self.cache[key] = [key[i] for i in sorted(keep)]
        res = self.cache[key]
        return res if key == fwd else res[::-1]

    def ring(self, ring, nodes):
        idx = [i for i, v in enumerate(ring) if v in nodes]
        if not idx:
            # 노드 없는 고리(섬, 다른 지역에 완전히 둘러싸인 지역): 가장 작은 점에서 시작해 정규화
            m = min(range(len(ring)), key=lambda i: ring[i])
            r = ring[m:] + ring[:m]
            if r[1] > r[-1]:
                r = [r[0]] + r[1:][::-1]
            pts = [self.proj(p) for p in r]
            f = far_index(pts, 0, len(r) - 1)
            closed = r + [r[0]]
            out = []
            for a, b in ((0, f), (f, len(r))):
                part = closed[a:b + 1]
                kept = self.arc(part)
                if len(kept) == 2 and len(part) > 2:  # 반쪽마다 한 점은 남겨 섬이 사라지지 않게
                    pp = [self.proj(p) for p in part]
                    ax, ay = pp[0]; bx, by = pp[-1]
                    mid = max(range(1, len(part) - 1),
                              key=lambda i: abs((bx - ax) * (pp[i][1] - ay) - (by - ay) * (pp[i][0] - ax)))
                    kept = [part[0], part[mid], part[-1]]
                out.extend(kept[:-1])
            return out
        r = ring[idx[0]:] + ring[:idx[0]]
        idx = [i - idx[0] for i in idx] + [len(ring)]
        closed = r + [r[0]]
        out = []
        for a, b in zip(idx, idx[1:]):
            out.extend(self.arc(closed[a:b + 1])[:-1])
        return out


# ------------------------------------------------------------------ 기하 도구
def area(pts):
    s = 0.0
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]
        x2, y2 = pts[(i + 1) % n]
        s += x1 * y2 - x2 * y1
    return s / 2


def bbox(rings):
    xs = [p[0] for r in rings for p in r]
    ys = [p[1] for r in rings for p in r]
    return min(xs), min(ys), max(xs), max(ys)


def inside(x, y, rings):  # even-odd
    c = False
    for r in rings:
        n = len(r)
        j = n - 1
        for i in range(n):
            xi, yi = r[i]
            xj, yj = r[j]
            if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                c = not c
            j = i
    return c


def seg_dist2(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    L = dx * dx + dy * dy
    t = 0 if L == 0 else max(0, min(1, ((px - ax) * dx + (py - ay) * dy) / L))
    return (px - ax - t * dx) ** 2 + (py - ay - t * dy) ** 2


def edge_dist(x, y, rings):
    best = float("inf")
    for r in rings:
        n = len(r)
        for i in range(n):
            a, b = r[i], r[(i + 1) % n]
            d = seg_dist2(x, y, a[0], a[1], b[0], b[1])
            if d < best:
                best = d
    return math.sqrt(best)


def label_point(rings):
    """가장 큰 고리 안에서 경계와 가장 먼 점(대략). 도넛 모양(완주 등)에서도 지역 안에 찍힌다."""
    big = max(rings, key=lambda r: abs(area(r)))
    x0, y0, x1, y1 = bbox([big])
    holes = [r for r in rings if r is not big]
    test = [big] + holes
    best, bp = -1, None
    cx, cy, w, h = x0, y0, x1 - x0, y1 - y0
    for step in range(3):
        g = 16
        for i in range(g + 1):
            for j in range(g + 1):
                x, y = cx + w * i / g, cy + h * j / g
                if not inside(x, y, test):
                    continue
                d = edge_dist(x, y, test)
                if d > best:
                    best, bp = d, (x, y)
        if bp is None:
            break
        w, h = w / 4, h / 4
        cx, cy = bp[0] - w / 2, bp[1] - h / 2
    if bp is None:
        bp = big[0]
    return bp


# ------------------------------------------------------------------ 투영
def merc(p):
    lon, lat = p
    return math.radians(lon), -math.log(math.tan(math.pi / 4 + math.radians(lat) / 2))


def fit(src_box, rect):
    x0, y0, x1, y1 = src_box
    rx0, ry0, rx1, ry1 = rect
    s = min((rx1 - rx0) / (x1 - x0), (ry1 - ry0) / (y1 - y0))
    ox = rx0 + ((rx1 - rx0) - (x1 - x0) * s) / 2 - x0 * s
    oy = ry0 + ((ry1 - ry0) - (y1 - y0) * s) / 2 - y0 * s
    return s, ox, oy


LAT0 = math.radians(36.0)


def meters(p):
    return p[0] * 111320.0 * math.cos(LAT0), p[1] * 110540.0


def rnd(v, d):
    r = round(v, d)
    return int(r) if r == int(r) else r


# ------------------------------------------------------------------ 메인
def main():
    gj = load_source()
    feats = gj["features"]
    sgg_info = {}
    emd_rings = []
    emd_sgg = {}
    for i, f in enumerate(feats):
        p = f["properties"]
        sgg_info.setdefault(p["sgg"], (p["sido"], p["sidonm"], p["sggnm"]))
        emd_sgg[i] = p["sgg"]
        for r in rings_of(f["geometry"]):
            emd_rings.append((i, r))

    reg = {}  # 161 지역 코드 → 정보
    sgg_reg = {}
    for sgg, (sido, sidonm, sggnm) in sorted(sgg_info.items()):
        rc, rname, gu = region_of(sgg, sido, sggnm)
        sgg_reg[sgg] = rc
        reg.setdefault(rc, {"code": rc, "name": rname, "sido": sidonm, "sidoCode": sido,
                            "group": GROUP_OF_SIDO[sido], "units": []})["units"].append(sgg)

    # ---------- 판정용: 시·군·구 단위
    sgg_rings, sgg_nodes = dissolve(emd_rings, emd_sgg)
    simp_l = Simplifier(meters, LOOKUP_TOL_M)
    units = []
    dropped_l = 0
    for sgg in sorted(sgg_rings):
        sido, sidonm, sggnm = sgg_info[sgg]
        rc, rname, gu = region_of(sgg, sido, sggnm)
        rings = [simp_l.ring(r, sgg_nodes) for r in sgg_rings[sgg]]
        rings_m = [[meters(p) for p in r] for r in rings]
        big = max(range(len(rings)), key=lambda i: abs(area(rings_m[i])))
        keep = [r for i, r in enumerate(rings) if i == big or abs(area(rings_m[i])) >= LOOKUP_MIN_AREA_M2]
        dropped_l += len(rings) - len(keep)
        lp = label_point(keep)
        units.append({
            "code": sgg, "name": gu if gu else sggnm, "fullName": sggnm, "sido": sidonm,
            "sidoShort": "광주" if sgg in GWANGJU_SGG else SIDO_SHORT[sido], "region": rc,
            "center": [rnd(lp[0], LOOKUP_DECIMALS), rnd(lp[1], LOOKUP_DECIMALS)],
            "bbox": [rnd(v, LOOKUP_DECIMALS) for v in bbox(keep)],
            "rings": [[rnd(c, LOOKUP_DECIMALS) for p in r for c in p] for r in keep],
        })

    # ---------- 화면용: 161 지역 단위
    reg_rings, reg_nodes = dissolve(emd_rings, {i: sgg_reg[s] for i, s in emd_sgg.items()})
    ULLEUNG, JEJU = "47940", {"50110", "50130"}

    # 옮길 섬 묶음: 해당 읍·면 고리들의 위경도 범위
    moved_box = {}
    for m in MOVED:
        rs = [r for i, r in emd_rings if emd_sgg[i] == m["sgg"]
              and feats[i]["properties"]["adm_nm"].split(" ")[-1] in m["emd"]]
        moved_box[m["id"]] = bbox(rs)

    def inset_of(rc, ring):
        if rc in JEJU:
            return "jeju"
        if rc == ULLEUNG:
            return "dokdo" if min(p[0] for p in ring) > 131.5 else "ulleung"
        x0, y0, x1, y1 = bbox([ring])
        for m in MOVED:
            bx0, by0, bx1, by1 = moved_box[m["id"]]
            if m["sgg"] in reg[rc]["units"] and bx0 <= x0 and x1 <= bx1 and by0 <= y0 and y1 <= by1:
                return m["id"]
        return "main"

    groups = defaultdict(list)
    for rc, rings in reg_rings.items():
        for r in rings:
            groups[inset_of(rc, r)].append([merc(p) for p in r])
    T = {
        "main": fit(bbox(groups["main"]), MAIN_RECT),
        "jeju": fit(bbox([max(groups["jeju"], key=lambda r: abs(area(r)))]), JEJU_RECT),
        "ulleung": fit(bbox([max(groups["ulleung"], key=lambda r: abs(area(r)))]), ULLEUNG_RECT),
        "dokdo": fit(bbox(groups["dokdo"]), DOKDO_RECT),
    }
    sm = T["main"][0]
    for m in MOVED:  # 본토와 같은 배율, 내용 중심을 m["center"]로
        x0, y0, x1, y1 = bbox(groups[m["id"]])
        T[m["id"]] = (sm, m["center"][0] - (x0 + x1) / 2 * sm, m["center"][1] - (y0 + y1) / 2 * sm)

    def mk_proj(name):
        s, ox, oy = T[name]

        def proj(p):
            x, y = merc(p)
            return x * s + ox, y * s + oy
        return proj

    projs = {k: mk_proj(k) for k in T}

    # 판정용 지역 중심점: 화면 배치와 무관하게 위경도 경계(판정용과 같은 단순화)에서 바로 구한다
    simp_rc = Simplifier(meters, LOOKUP_TOL_M)

    def region_center(rc):
        rings = [simp_rc.ring(r, reg_nodes) for r in reg_rings[rc]]
        rm = [[meters(p) for p in r] for r in rings]
        big = max(range(len(rm)), key=lambda i: abs(area(rm[i])))
        x, y = label_point([rm[big]] + [r for i, r in enumerate(rm) if i != big and abs(area(r)) >= LOOKUP_MIN_AREA_M2])
        return [rnd(x / (111320.0 * math.cos(LAT0)), LOOKUP_DECIMALS), rnd(y / 110540.0, LOOKUP_DECIMALS)]
    simps = {k: Simplifier(projs[k], DISPLAY_TOL) for k in T}
    def to_d(rings):
        d = " ".join("M" + " ".join(f"{rnd(p[0], DISPLAY_DECIMALS)} {rnd(p[1], DISPLAY_DECIMALS)}" if k != 1 else
                                    f"L{rnd(p[0], DISPLAY_DECIMALS)} {rnd(p[1], DISPLAY_DECIMALS)}"
                                    for k, p in enumerate(r)) + "Z" for r in rings)
        return d.replace(" L", "L")

    regions = []
    island_stats = {}
    ring_insets = defaultdict(list)  # 인셋 → [(지역, 화면 고리)]
    for rc in sorted(reg):
        info = reg[rc]
        src = reg_rings[rc]
        out, ins_of = [], []
        for r in src:
            ins = inset_of(rc, r)
            sr = simps[ins].ring(r, reg_nodes)
            out.append([projs[ins](p) for p in sr])
            ins_of.append(ins)
        ar = [abs(area(r)) for r in out]
        big = max(range(len(out)), key=lambda i: ar[i])
        keep_i = [i for i in range(len(out)) if i == big or ar[i] >= DISPLAY_MIN_AREA
                  or rc == ULLEUNG]  # 울릉군(독도 포함)은 크기와 무관하게 모두 남김
        kept = [out[i] for i in keep_i]
        for i in keep_i:
            ring_insets[ins_of[i]].append((rc, out[i]))
        island_stats[rc] = (len(src), len(kept))
        lp = label_point(kept)
        d = to_d(kept)
        regions.append({
            "code": rc, "name": info["name"], "sido": info["sido"], "group": info["group"],
            "inset": "ulleung" if rc == ULLEUNG else ins_of[big],
            "center": [rnd(lp[0], DISPLAY_DECIMALS), rnd(lp[1], DISPLAY_DECIMALS)], "d": d,
        })

    # 시·도 묶음 라벨 자리: 묶음 경계(본토 부분) 안에서 경계와 가장 먼 점. 서울(경기 안)·제주(섬 박스 라벨)는 없음
    grp_rings, grp_nodes = dissolve(emd_rings, {i: GROUP_OF_SIDO[sgg_info[s][0]] for i, s in emd_sgg.items()})
    simp_g = Simplifier(projs["main"], 1.0)
    group_out = []
    for gid, gname, _ in GROUPS:
        label = None
        if gid not in ("seoul", "jj"):
            rings = [[projs["main"](p) for p in simp_g.ring(r, grp_nodes)] for r in grp_rings[gid]]
            rings = [r for r in rings if len(r) >= 3]
            big = max(rings, key=lambda r: abs(area(r)))
            holes = [r for r in rings if r is not big and abs(area(r)) > 20]
            lp = label_point([big] + holes)
            label = [rnd(lp[0], 1), rnd(lp[1], 1)]
        group_out.append({"id": gid, "name": gname, "regions": sum(1 for r in regions if r["group"] == gid),
                          "labelPos": label})

    def content_box(*ids):
        pts = [p for i in ids for _, r in ring_insets[i] for p in r]
        return min(p[0] for p in pts), min(p[1] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts)

    def union(a, b, pad):
        return [rnd(min(a[0], b[0] - pad), 1), rnd(min(a[1], b[1] - pad), 1),
                rnd(max(a[2], b[2] + pad), 1), rnd(max(a[3], b[3] + pad), 1)]

    def pad_box(b, pad):
        return [rnd(b[0] - pad, 1), rnd(b[1] - pad, 1), rnd(b[2] + pad, 1), rnd(b[3] + pad, 1)]

    def label_rect(text, x, y, anchor, size=12):
        w = size * 0.95 * len(text)
        x0 = x - w / 2 if anchor == "middle" else x
        return (x0, y - size, x0 + w, y + 2)

    def overlaps(a, b):
        return a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3]

    main_pts = [p for _, r in ring_insets["main"] for p in r]

    def hits_main(box):
        return sum(1 for x, y in main_pts if box[0] <= x <= box[2] and box[1] <= y <= box[3])

    # 제주 박스가 남해안과 겹치면 추자도(제주 본섬 북쪽 고리들)를 박스 위쪽으로 당긴다(거리 압축)
    jeju_frame = union(JEJU_FRAME, content_box("jeju"), 4)
    chuja_pulled = False
    if hits_main(jeju_frame):
        main_top = min(p[1] for rc, r in ring_insets["jeju"] for p in r if abs(area(r)) == max(
            abs(area(q)) for c, q in ring_insets["jeju"] if c == rc))
        north = [(rc, r) for rc, r in ring_insets["jeju"] if max(p[1] for p in r) < main_top - 2]
        dy = (main_top - 8) - max(p[1] for _, r in north for p in r)
        for rc, r in north:
            r[:] = [(x, y + dy) for x, y in r]
        chuja_pulled = True
        jeju_frame = union(JEJU_FRAME, content_box("jeju"), 4)
        # 옮긴 고리로 d 다시 만들기
        for reg_out in regions:
            if reg_out["code"] in JEJU:
                rings = [r for c, r in ring_insets["jeju"] if c == reg_out["code"]]
                reg_out["d"] = to_d(rings)

    insets = [
        {"id": "ulleung", "regions": [ULLEUNG], "label": LABELS["ulleung"][0],
         "labelPos": list(LABELS["ulleung"][1:3]), "labelAnchor": LABELS["ulleung"][3],
         "frame": union(ULLEUNG_FRAME, content_box("ulleung", "dokdo"), 4)},
        {"id": "jeju", "regions": sorted(JEJU), "label": LABELS["jeju"][0],
         "labelPos": list(LABELS["jeju"][1:3]), "labelAnchor": LABELS["jeju"][3], "frame": jeju_frame,
         "compressed": chuja_pulled},
    ]
    for m in MOVED:
        insets.append({"id": m["id"], "regions": sorted({rc for rc, _ in ring_insets[m["id"]]}),
                       "label": m["label"][0], "labelPos": list(m["label"][1:3]), "labelAnchor": m["label"][3],
                       "frame": pad_box(content_box(m["id"]), 6),
                       "emd": [f"{reg[rc]['name']} {e}" for rc in sorted({rc for rc, _ in ring_insets[m['id']]})
                               for e in m["emd"]]})

    # 겹침 검사: 박스·라벨 ↔ 본토, 박스·라벨 ↔ 지도 UI, 박스 ↔ 박스
    things = [(f"{i['id']}.frame", i["frame"]) for i in insets] + \
             [(f"{i['id']}.label", label_rect(i["label"], *i["labelPos"], i["labelAnchor"])) for i in insets]
    problems = []
    for name, box in things:
        n = hits_main(box)
        if n:
            problems.append(f"{name} ↔ 본토 꼭짓점 {n}개")
        for un, ub in UI_RECTS.items():
            if overlaps(box, ub):
                problems.append(f"{name} ↔ {un}")
    for i, (an, ab) in enumerate(things):
        for bn, bb in things[i + 1:]:
            if an.split(".")[0] != bn.split(".")[0] and overlaps(ab, bb):
                problems.append(f"{an} ↔ {bn}")
    mb = content_box("main")
    print(f"본토 범위 x {mb[0]:.0f}~{mb[2]:.0f}, y {mb[1]:.0f}~{mb[3]:.0f} (세로 {mb[3] - mb[1]:.0f})")
    print("제주 추자도 당김:", chuja_pulled)
    print("겹침:", problems or "없음")
    for i in insets:
        print(" ", i["id"], "frame", i["frame"])
    if problems:
        sys.exit("배치 겹침 → MOVED center/label 조정 필요")

    meta = {
        "source": f"vuski/admdongkor {SRC_VERSION} (commit {SRC_COMMIT[:12]})",
        "basedOn": "통계청 SGIS 행정동 경계",
        "license": "CC BY 4.0 (가공물) · 공공누리 제1유형(출처표시)",
        "generatedBy": "tools/geo/build_regions.py",
    }
    display = {
        "meta": meta | {"projection": "Mercator, 본토와 섬 박스는 각각 다른 배율",
                        "fillRule": "evenOdd", "scale": {k: rnd(v[0] / T["main"][0], 2) for k, v in T.items()},
                        "uiRects": UI_RECTS},
        "viewBox": [VIEW_W, VIEW_H], "groups": group_out, "insets": insets, "regions": regions,
    }
    lookup = {
        "meta": meta | {"crs": "WGS84 lon,lat", "fillRule": "evenOdd",
                        "tolerance_m": LOOKUP_TOL_M, "ringFormat": "flat [lon,lat,lon,lat,…]"},
        "regions": [{"code": r["code"], "name": r["name"], "sido": r["sido"], "group": r["group"],
                     "units": reg[r["code"]]["units"], "center": region_center(r["code"])} for r in regions],
        "units": units,
    }
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, obj in (("regions_display.json", display), ("regions_lookup.json", lookup)):
        path = os.path.join(OUT_DIR, name)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, separators=(",", ":"))
        print(f"{name}: {os.path.getsize(path) / 1024:.0f} KB")
    print("regions", len(regions), "units", len(units), "lookup small rings dropped", dropped_l)
    with open(os.path.join(CACHE, "island_stats.json"), "w", encoding="utf-8") as f:
        json.dump(island_stats, f, ensure_ascii=False)


if __name__ == "__main__":
    main()
