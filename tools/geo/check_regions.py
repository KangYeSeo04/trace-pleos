#!/usr/bin/env python3
"""regions_*.json 정확도 확인 + 미리보기 PNG.

실행:  python3 tools/geo/check_regions.py   (build_regions.py 다음에. Pillow가 사용자 site에 있으면 -I 없이)
의존성: 표준 라이브러리 + Pillow(미리보기 PNG만). Pillow가 없으면 PNG는 건너뛴다.
출력: 표준 출력에 확인 표, tools/geo/preview/regions_preview.png
"""
import json
import os
import re
from collections import Counter, defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
CACHE = os.path.join(ROOT, "tools", "geo", ".cache")
PREVIEW = os.path.join(ROOT, "tools", "geo", "preview", "regions_preview.png")

# 디자인(RegionDark.dc.html)의 묶음 색 C, 지도 색 mapC(dark)
GROUP_COLOR = {"gg": "#7FA8F5", "gw": "#6ED3AC", "cb": "#F0B45A", "cn": "#E3CC6E", "gb": "#B99BF0",
               "gn": "#F28BA9", "jb": "#6CC9D8", "jn": "#A2D47E", "jj": "#F4A07E", "seoul": "#6A4BD8"}
SEA, LINE, LABEL = "#1B2433", "#A9AFB8", "#8E949D"

# 알려진 위치 → 기대 시·군·구 코드 (판정용 데이터로 점-다각형 검사)
PROBES = [
    ("서울시청", 126.9780, 37.5665, "11140"),
    ("인천공항(영종도)", 126.4407, 37.4602, "28"),       # 영종구
    ("동인천역", 126.6326, 37.4754, "28"),               # 제물포구
    ("검단신도시", 126.6750, 37.6000, "28"),             # 검단구
    ("군위군청", 128.5730, 36.2428, "27"),               # 대구 군위군
    ("정부세종청사", 127.2620, 36.5040, "36110"),
    ("광주시청", 126.8526, 35.1600, "12"),               # 전남광주 서구 → 지역 '광주'
    ("독도", 131.8650, 37.2410, "47940"),
    ("울릉군청", 130.9057, 37.4844, "47940"),
    ("마라도", 126.2670, 33.1180, "50130"),
    ("추자도", 126.3000, 33.9550, "50110"),
    ("백령도", 124.7150, 37.9600, "28"),                 # 옹진군
    ("흑산도", 125.4300, 34.6800, "12870"),              # 신안군
    ("전주시청(완주군에 둘러싸임)", 127.1480, 35.8242, "52"),
]


def inside(x, y, flat):
    c = False
    n = len(flat) // 2
    j = n - 1
    for i in range(n):
        xi, yi, xj, yj = flat[2 * i], flat[2 * i + 1], flat[2 * j], flat[2 * j + 1]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            c = not c
        j = i
    return c


def lookup(units, lon, lat):
    hits = []
    for u in units:
        x0, y0, x1, y1 = u["bbox"]
        if not (x0 <= lon <= x1 and y0 <= lat <= y1):
            continue
        c = False
        for r in u["rings"]:
            if inside(lon, lat, r):
                c = not c
        if c:
            hits.append(u)
    return hits


def parse_d(d):
    rings = []
    for part in d.split("M")[1:]:
        nums = [float(t) for t in re.split(r"[ LZ]+", part.strip()) if t]
        rings.append(list(zip(nums[0::2], nums[1::2])))
    return rings


def area(r):
    return abs(sum(r[i][0] * r[(i + 1) % len(r)][1] - r[(i + 1) % len(r)][0] * r[i][1] for i in range(len(r)))) / 2


def main():
    disp = json.load(open(os.path.join(ASSETS, "regions_display.json"), encoding="utf-8"))
    look = json.load(open(os.path.join(ASSETS, "regions_lookup.json"), encoding="utf-8"))
    regions, units = disp["regions"], look["units"]
    print("## 크기")
    for n in ("regions_display.json", "regions_lookup.json"):
        print(f"- {n}: {os.path.getsize(os.path.join(ASSETS, n)) / 1024:.0f} KB")

    print(f"\n## 개수: 161 지역 {len(regions)}곳 / 시·군·구 {len(units)}개")
    print("| 시·도 | 시·군·구(일반구 포함) | 161 지역 |")
    print("|---|---|---|")
    u_by = Counter(u["sido"] for u in units)
    r_by = Counter(r["sido"] for r in regions)
    for s in sorted(u_by, key=lambda s: units[[u["sido"] for u in units].index(s)]["code"]):
        print(f"| {s} | {u_by[s]} | {r_by[s]} |")
    print("\n| 묶음(범례) | 지역 수 |")
    print("|---|---|")
    for g in disp["groups"]:
        print(f"| {g['name']} | {g['regions']} |")

    print("\n## 최근 변경·특수 지역")
    names = {u["code"]: u for u in units}
    def has(pred):
        return [f"{u['sidoShort']} {u['name']}({u['code']})" for u in units if pred(u)]
    print("- 대구 군위군:", has(lambda u: u["name"] == "군위군"))
    print("- 인천 시·군·구:", [u["name"] for u in units if u["sido"] == "인천광역시"])
    print("- 세종:", has(lambda u: u["sido"].startswith("세종")))
    print("- 제주:", has(lambda u: u["sido"].startswith("제주")))
    print("- 광주(161 지역 12200) 구성:", [names[c]["name"] for c in next(r for r in look["regions"] if r["code"] == "12200")["units"]])
    ul = next(r for r in regions if r["code"] == "47940")
    ul_r = parse_d(ul["d"])
    print(f"- 울릉군 화면 고리 {len(ul_r)}개, 독도 박스(x>515) 고리 {sum(1 for r in ul_r if min(p[0] for p in r) > 515)}개")

    print("\n## 위치 판정 확인 (판정용 데이터)")
    print("| 위치 | 판정 | 기대 | 결과 |")
    print("|---|---|---|---|")
    ok = 0
    for name, lon, lat, exp in PROBES:
        h = lookup(units, lon, lat)
        got = ", ".join(f"{u['sidoShort']} {u['name']}→{u['region']}" for u in h) or "없음"
        good = len(h) == 1 and h[0]["code"].startswith(exp)
        ok += good
        print(f"| {name} | {got} | {exp}… | {'OK' if good else '확인 필요'} |")
    print(f"{ok}/{len(PROBES)} 일치")

    print("\n## 섬 보존 (원본 고리 → 화면용 남은 고리, 판정용 남은 고리)")
    stats = json.load(open(os.path.join(CACHE, "island_stats.json"), encoding="utf-8"))
    lk = defaultdict(int)
    for u in units:
        lk[u["region"]] += len(u["rings"])
    rmap = {r["code"]: r for r in regions}
    print("| 지역 | 원본 | 화면용 | 판정용 |")
    print("|---|---|---|---|")
    top = sorted(stats.items(), key=lambda kv: -kv[1][0])[:10]
    for code, (src, kept) in top:
        print(f"| {rmap[code]['name']} | {src} | {kept} | {lk[code]} |")
    tot_s = sum(v[0] for v in stats.values())
    tot_k = sum(v[1] for v in stats.values())
    print(f"| 전체 | {tot_s} | {tot_k} | {sum(lk.values())} |")

    render(disp)


def render(disp, k=2):
    try:
        from PIL import Image, ImageDraw, ImageFont
    except ImportError:
        print("Pillow 없음: 미리보기 PNG 건너뜀")
        return
    W, H = disp["viewBox"]
    img = Image.new("RGB", (int(W * k), int(H * k)), SEA)
    dr = ImageDraw.Draw(img)
    for ins in disp["insets"]:
        x0, y0, x1, y1 = ins["frame"]
        dr.rectangle([x0 * k, y0 * k, x1 * k, y1 * k], outline=LINE, width=max(1, int(1.2 * k)))
    regs = [(r, parse_d(r["d"])) for r in disp["regions"]]
    regs.sort(key=lambda t: -sum(area(x) for x in t[1]))  # 큰 지역 먼저 → 둘러싸인 지역(전주 등)이 위에
    for r, rings in regs:
        for ring in sorted(rings, key=area, reverse=True):
            dr.polygon([(x * k, y * k) for x, y in ring], fill=GROUP_COLOR[r["group"]])
    for r, rings in regs:
        for ring in rings:
            pts = [(x * k, y * k) for x, y in ring]
            dr.line(pts + [pts[0]], fill="#1F2124", width=1)
    font_path = os.path.join(ROOT, "design", "fonts", "pretendard_bold.otf")
    try:
        f16 = ImageFont.truetype(font_path, int(16 * k))
        f12 = ImageFont.truetype(font_path, int(12 * k))
    except OSError:
        f16 = f12 = ImageFont.load_default()
    for ins in disp["insets"]:
        x, y = ins["labelPos"]
        dr.text((x * k, y * k), ins["label"], fill=LABEL, font=f12, anchor="ms" if ins["id"] == "ulleung" else "ls")
    # 묶음 이름: 각 묶음에서 가장 큰 본토 지역 중심에(확인용 표시, 실제 라벨 자리는 화면 단계에서 디자인대로)
    seen = set()
    for r, rings in regs:
        if r["group"] in seen or r["inset"] != "main":
            continue
        seen.add(r["group"])
        name = next(g["name"] for g in disp["groups"] if g["id"] == r["group"])
        x, y = r["center"]
        dr.text((x * k, y * k), name, fill="#111418", font=f16, anchor="mm")
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    img.save(PREVIEW, optimize=True)
    print(f"\n미리보기: {os.path.relpath(PREVIEW, ROOT)} ({img.width}×{img.height})")


if __name__ == "__main__":
    main()
