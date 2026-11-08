#!/usr/bin/env python3
"""
사자 카트 시연용 시나리오 재생기 (PC에서 실행, 앱은 SDK로 값을 받기만 한다).

Pleos Connect 에뮬레이터는 속도를 줘도 주행거리·배터리가 저절로 변하지 않는다.
그래서 가상 콘솔 API(127.0.0.1:8088, adb forward tcp:8088 tcp:8088 필요)를
JSON 시나리오의 시간 순서대로 호출한다.

  python3 tools/scenario/replay.py tools/scenario/sample_30s.json
  python3 tools/scenario/replay.py commute.json --adb-mark   # logcat PROBE에 STEP 표시

시나리오 형식
{
  "name": "...",
  "auto": {                     # 선택: 속도로 주행거리·배터리·주행가능거리를 매 tick 계산
    "km_per_kwh": 6.0,          # 전비
    "hvac_kw": 1.5,             # 공조 켜져 있을 때 추가 소비 전력
    "speedup": 20,              # 거리·소비만 n배 빠르게 (짧은 시연용, 1이면 실제 시간)
    "tick_s": 1.0
  },
  "steps": [
    {"t": 0, "gear": "Park", "speed_kph": 0, "soc": 80, "battery_kwh": 61.9, "odometer_km": 12345,
     "range_m": 371000, "hvac": {"power": false}, "turn_signal": "Off", "note": "출발 전"},
    {"t": 5, "gear": "Drive", "speed_kph": 30}
  ]
}
step 키는 모두 선택이고, 없는 키는 직전 값을 유지한다.
"""
import argparse
import json
import subprocess
import sys
import threading
import time
import urllib.parse
import urllib.request

BASE = "http://127.0.0.1:8088"


def put(path, **params):
    q = urllib.parse.urlencode({k: str(v).lower() if isinstance(v, bool) else v for k, v in params.items()})
    req = urllib.request.Request(f"{BASE}{path}?{q}", method="PUT")
    with urllib.request.urlopen(req, timeout=5) as r:
        r.read()
    return f"{path}?{q}"


def get_state():
    with urllib.request.urlopen(f"{BASE}/vehicle/state", timeout=5) as r:
        return json.load(r)


def heartbeat(stop):
    # 브라우저 세션이 없으면 콘솔이 값을 내보내지 않으므로 1초마다 신호를 보낸다
    while not stop.is_set():
        try:
            urllib.request.urlopen(urllib.request.Request(
                f"{BASE}/browser/session?client=scenario", method="POST"), timeout=2).read()
        except Exception:
            pass
        stop.wait(1)


class Car:
    def __init__(self, st, mark):
        c, o, e = st["controls"], st["odometer"], st["ev_battery"]
        self.gear = st["gear"]["actual"]["value"]
        self.controls = {"speed": c["speed_kph"], "accelerator": c["accelerator_pct"],
                         "brake": c["brake_pct"], "steering": c["steering_deg"]}
        self.odo = float(o["traveled_distance_km"])
        self.capacity = float(e["maximum_capacity_kwh"])
        self.energy = float(e["current_energy_kwh"])
        self.hvac_power = bool(st["hvac"]["power_on"])
        self.mark = mark

    def log(self, t, text):
        print(f"[{t:6.1f}s] {text}", flush=True)
        if self.mark:
            subprocess.run(["adb", "shell", "log", "-t", "PROBE", f"STEP {text}"], capture_output=True)

    def apply(self, t, s):
        if "gear" in s:
            self.gear = s["gear"]
            put("/vehicle/gear", gear=self.gear)
        keys = {"speed_kph": "speed", "accelerator": "accelerator", "brake": "brake", "steering": "steering"}
        if any(k in s for k in keys):
            for k, v in keys.items():
                if k in s:
                    self.controls[v] = s[k]
            put("/vehicle/controls", **self.controls)
        if "odometer_km" in s:
            self.odo = float(s["odometer_km"])
            put("/vehicle/odometer", traveled_distance_km=self.odo)
        if "range_m" in s:
            put("/vehicle/odometer", remaining_range_m=s["range_m"])
        if "battery_kwh" in s:
            self.energy = float(s["battery_kwh"])
            put("/vehicle/ev-battery", field="battery_level", value=self.energy)
        if "soc" in s:
            put("/vehicle/ev-battery", field="soc", value=s["soc"])
        if "hvac" in s:
            self.hvac_power = bool(s["hvac"].get("power", self.hvac_power))
            put("/vehicle/hvac", **s["hvac"])
        if "turn_signal" in s:
            put("/vehicle/turn-signal", mode=s["turn_signal"])
        changed = {k: v for k, v in s.items() if k not in ("t", "note")}
        self.log(t, f"{s.get('note', '')} {json.dumps(changed, ensure_ascii=False)}".strip())

    def tick(self, dt, auto):
        """속도로 주행거리·에너지를 적분해 콘솔에 반영 (auto 모드)"""
        sim_dt = dt * auto.get("speedup", 1)
        dist = self.controls["speed"] * sim_dt / 3600.0 if self.gear == "Drive" else 0.0
        used = dist / auto["km_per_kwh"] + (auto.get("hvac_kw", 0) * sim_dt / 3600.0 if self.hvac_power else 0)
        if dist == 0 and used == 0:
            return
        self.odo += dist
        self.energy = max(0.0, self.energy - used)
        put("/vehicle/odometer", traveled_distance_km=round(self.odo, 3))
        put("/vehicle/ev-battery", field="battery_level", value=round(self.energy, 3))
        put("/vehicle/ev-battery", field="soc", value=round(self.energy / self.capacity * 100))
        put("/vehicle/odometer", remaining_range_m=round(self.energy * auto["km_per_kwh"] * 1000))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("scenario")
    ap.add_argument("--adb-mark", action="store_true", help="logcat PROBE 태그에 STEP 줄 남기기")
    args = ap.parse_args()
    sc = json.load(open(args.scenario, encoding="utf-8"))
    steps = sorted(sc["steps"], key=lambda s: s["t"])
    auto = sc.get("auto")

    stop = threading.Event()
    threading.Thread(target=heartbeat, args=(stop,), daemon=True).start()
    try:
        car = Car(get_state(), args.adb_mark)
    except Exception as e:
        sys.exit(f"가상 콘솔에 연결할 수 없음 ({e}). adb forward tcp:8088 tcp:8088 확인")
    print(f"시나리오: {sc.get('name', args.scenario)} ({steps[-1]['t']}s)", flush=True)

    start = time.monotonic()
    tick_s = auto.get("tick_s", 1.0) if auto else None
    last_tick = start
    i = 0
    try:
        while i < len(steps):
            now = time.monotonic()
            t = now - start
            while i < len(steps) and steps[i]["t"] <= t:
                car.apply(steps[i]["t"], steps[i])
                i += 1
            if auto and now - last_tick >= tick_s:
                car.tick(now - last_tick, auto)
                last_tick = now
            time.sleep(0.05)
    finally:
        stop.set()
    print(f"끝: odo={car.odo:.3f}km energy={car.energy:.3f}kWh", flush=True)


if __name__ == "__main__":
    main()
