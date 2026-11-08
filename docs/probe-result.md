# 사자 카트 차량 데이터 확인 결과 (PROBE)

- 확인 일시: 2026-11-02 (에뮬레이터 시계 기준 11-01 20:55~20:57)
- 에뮬레이터: Pleos Connect v2.1.0 (API 34, user 10), Cold Boot 직후
- SDK: Vehicle 2.3.2 / NaviHelper 3.0.5 / SpeechToText 3.3.1 / TextToSpeech 3.3.1 / FusedLocation 1.1.3
- 화면: 런처의 **PROBE** 아이콘, 또는 `adb shell am start --user 10 -n com.tracers.trace/.probe.ProbeActivity`
- 로그: `adb logcat -s PROBE` (형식: `수신시각(ms) · 항목 · 값 (+간격ms)`)
- 증거 파일: [probe-evidence/](probe-evidence/)
  - `probe_log_sequence.txt`: 전체 로그. `STEP …` 줄이 콘솔에서 값을 바꾼 시점이다.
  - `console_sequence.sh`: 값을 바꾸는 데 쓴 콘솔 명령 순서
  - 스크린샷: `01_park_baseline.png`, `02_after_sequence.png`, `03_overlay_chip.png`

결과 칸의 뜻:

- **가능:** 값이 읽히고, 콘솔에서 값을 바꾸면 콜백도 온다.
- **불가:** API 자체가 없다.
- **권한 오류:** SecurityException 또는 "need Permission" 오류가 난다.
- **에뮬레이터 미지원:** config가 null이거나 `ERROR_NOT_SUPPORTED_PROPERTY` / `property ID is not in carPropertyConfig list` 오류가 난다.

> 중요: 에뮬레이터의 차는 **전기차(EV)** 로 설정돼 있다. 가상 콘솔(`/vehicle/catalog`)에는 연료 관련 항목이 없고 `ev-battery` 항목만 있다. 그래서 Fuel 계열이 전부 미지원으로 나온다.

## 0. 권한 부여 결과

| 권한 | 결과 | 비고 |
|---|---|---|
| pleos CAR_ENERGY / CLIMATE / SAFETY / WINDOWS / DRIVE / LIGHTS | 허용 | 설치할 때 `-g`로 줌 (런타임 권한) |
| pleos CAR_INFO / DOORS / SEATS / MIRRORS / TOLL | 허용 | |
| pleos NAVI_ROUTE / STT_SERVICE / TTS_SERVICE | 허용 | |
| android.car CAR_SPEED / CAR_POWERTRAIN / CAR_ENERGY / CAR_INFO | 허용 | |
| android.car CAR_MILEAGE / CONTROL_CAR_CLIMATE / CAR_ENGINE_DETAILED / READ_WINDSHIELD_WIPERS | **거부** | 시스템 권한이라 서드파티 앱은 받을 수 없음 |
| pleos.car.permission.CAR_SPEED | 거부 | 기기에 정의되지 않은 권한. 사고 리플레이 때 넣었던 것이고, 쓰는 곳 없음 |

## A. AAOS CarPropertyManager

| 속성 | 결과 | 지원(access/change) | 읽은 값 | 콜백 간격 | 비고 |
|---|---|---|---|---|---|
| PERF_VEHICLE_SPEED | 가능 | READ / CONTINUOUS, 10~20Hz | 0 → 30 → 20 → 10 → 0 km/h | 중앙값 100ms | 콘솔 값을 그대로 따라감 |
| GEAR_SELECTION | 가능 | READ / ONCHANGE | 4(P) → 8(D) → 4 | 바뀔 때만 | |
| FUEL_LEVEL | 가능 (값은 고정) | READ / CONTINUOUS, 1~100Hz | 15000.0 ml 고정 | 중앙값 18ms | 콘솔에 연료 항목이 없어 기본값만 나옴. 이 주기로 받으면 로그가 넘치므로 실제 앱에서는 rate를 낮출 것 |
| FUEL_LEVEL_LOW | 가능 (값은 고정) | READ / ONCHANGE | false | - | |
| INFO_FUEL_CAPACITY | 가능 (값은 고정) | READ / STATIC | 15000.0 ml | - | 연료량과 같아서 항상 "100%"가 됨 |
| INFO_FUEL_TYPE | 가능 (값은 고정) | READ / STATIC | [10] | - | 10 = FuelType.ELECTRIC (AOSP 상수 기준) |
| PERF_ODOMETER | 권한 오류 | config=null | - | - | CAR_MILEAGE 거부. **대체:** Vehicle SDK의 Odometer |
| HVAC_AC_ON | 권한 오류 | config=null | - | - | CONTROL_CAR_CLIMATE 거부. **대체:** Vehicle SDK의 HVAC |
| ENGINE_OIL_LEVEL | 권한 오류 / 에뮬레이터 미지원 | config=null | - | - | SDK 쪽도 "not in carPropertyConfig list" |
| WINDSHIELD_WIPERS_STATE | 권한 오류 | config=null | - | - | READ_WINDSHIELD_WIPERS 거부. **대체:** Vehicle SDK의 Wiper |

## B. Pleos Vehicle SDK

### B-1. checkXXXCapabilities

| 도메인 | 결과 | 비고 |
|---|---|---|
| Brake | 가능 | BrakePedalStatus READ |
| CarInfo | 가능 | VIN, CRN 둘 다 READ |
| Display | 가능 | TemperatureDisplayUnit READ_WRITE |
| Door | 가능 | DoorLock, DoorPosition(0~1), GloveBoxLock |
| DrivingMode | 일부 | CruiseControlState, TargetSpeed는 READ. DrivingModeState는 미지원 |
| EvBattery | 일부 | StateOfCharge, ChargePercentLimit(50~100), CurrentDrawLimit, ChargePort, ChargeSwitch는 됨. Ev 효율 4종은 미지원 |
| Fuel | **에뮬레이터 미지원** | 5개 전부 `ERROR_NOT_SUPPORTED_PROPERTY` (차량이 EV) |
| HVAC | 대부분 가능 | Ac, Power, Auto, Dual, Recirc, FanSpeed(1~8), Temperature(15~30), Defroster 등. MaxAc만 미지원 |
| Light | 일부 | 실내 무드등, 하이빔·로우빔, 테일램프, 실내등, 독서등은 됨. 안개등과 AmbientColorAuto는 미지원 |
| Odometer | 일부 | CurrentDistanceDriven만 READ. Trip 거리·시간 9종 전부 미지원 |
| Safety | 일부 | Airbag, SeatBuckled, PowerDownWarningIndicator는 됨. 엔진오일·브레이크액·변속기오일·워셔액·SeatNotBuckledWarning·AirbagDeployed는 미지원. **Telltales는 권한 오류** |
| Seat | 가능 | 위치, 이동, 열선·통풍, 착석, 안전벨트 |
| SideMirror | 가능 | 열선 0~3 |
| SteeringWheel | 가능 | 각도 READ, 범위 -470~500. 열선 0~3 |
| Tire | 일부 | LowTirePressure, WheelArea는 됨. **TirePressure는 권한 오류** (PRIVATE_CAR_SAFETY) |
| Toll | 에뮬레이터 미지원 | EtcsVoiceGuidanceVolume |
| TurnSignal | 가능 | READ |
| Window | 일부 | 창문 위치·잠금, 블라인드는 됨. SmartGlass 3종은 미지원 |
| Wiper | 가능 | WiperState READ: 앞·뒤 각각 [OFF, ON, SERVICE]. WiperSwitch READ_WRITE |

### B-2. 값 읽기 (콘솔에서 바꾼 뒤 콜백이 왔는지까지 확인)

| 항목 | SDK 메서드 | 결과 | 값 | 콜백 | 비고 |
|---|---|---|---|---|---|
| 연료량 | Fuel.getFuelPercent | 에뮬레이터 미지원 | - | - | 0x21400be5 not supported |
| 탱크 용량 | 없음 | 불가 | - | - | SDK에 API 자체가 없음 |
| 순간 연비 | Fuel.getInstantaneousFuelEconomy | 에뮬레이터 미지원 | - | - | 0x21600be0 |
| 이번 주행 평균 연비 | Fuel.getCurrentFuelEconomy | 에뮬레이터 미지원 | - | - | ERROR_NOT_SUPPORTED_PROPERTY |
| 주유 후 평균 연비 | Fuel.getSinceChargeFuelEconomy | 에뮬레이터 미지원 | - | - | 0x21600be1 |
| 누적 평균 연비 | Fuel.getCumulativeFuelEconomy | 에뮬레이터 미지원 | - | - | 0x21600be2 |
| 에어컨 | HVAC.getHvacAcState | **가능** | true → false → true | 바뀔 때마다 옴 | 영역 SeatArea(ROW_1_LEFT+…+ROW_2_RIGHT). 콘솔 `/vehicle/hvac?ac=` |
| 총 주행거리 | Odometer.getCurrentDistanceDriven | **가능** | 0.0 → 12345.0 | 약 4초마다 반복해서 옴 | 콘솔 `/vehicle/odometer?traveled_distance_km=` |
| 이번 주행거리 | Odometer.getTripCurrentDrivingDistance | 에뮬레이터 미지원 | - | - | |
| 이번 주행시간 | Odometer.getTripCurrentDrivingTime | 에뮬레이터 미지원 | - | - | 콘솔에서 usage_time_min=42로 바꿔도 반영 안 됨 |
| 엔진오일 | Safety.getEngineOilLevelState | 에뮬레이터 미지원 | - | - | |
| 브레이크액 | Safety.getBrakeFluidLevelState | 에뮬레이터 미지원 | - | - | 0x21400730 |
| 변속기오일 | Safety.getTransmissionOilLevelState | 에뮬레이터 미지원 | - | - | 0x21400731 |
| 워셔액 부족 | Safety.hasLowWasherFluidWarning | 에뮬레이터 미지원 | - | - | 0x2120071e |
| 파워다운 경고 | Safety.hasPowerDownWarningIndicator | 가능 | false | - | |
| 경고등 (조회) | Safety.getTelltalesInfo | **권한 오류** | - | - | "getTelltalesInfo need Permission". 어노테이션은 CAR_SAFETY인데, 그 권한을 받은 상태에서도 거부됨 |
| 경고등 (구독) | Safety.registerTelltales | **권한 오류** | - | - | PRIVATE_CAR_SAFETY 필요 |
| 와이퍼 상태 | Wiper.getCurrentWiperState(FRONT_WINDSHIELD) | **가능** | OFF → ON → OFF | 바뀔 때마다 옴 | 콘솔에서 Low로 바꾸면 ON. 단계(Low/High)는 State로는 안 나옴 |
| 브레이크 페달 | Brake.getCurrentBrakePedalStatus | **가능** | false → true → false | 바뀔 때마다 옴 | **Boolean on/off만** 옴. 콘솔의 brake 50%·100%는 둘 다 true |
| 조향각 | SteeringWheel.getCurrentSteeringWheelAngle | **가능** | 0 → **-90** → 0 | 약 3.1초마다 반복 + 바뀔 때 | 콘솔 steering=90이 **-90으로 부호가 반대**로 옴 |
| 방향지시등 | TurnSignal.getCurrentTurnSignalState | **가능** | OFF → LEFT → OFF | 바뀔 때마다 옴 | |
| 기어 | Gear.getCurrentGearSelectionState | **가능** | GEAR_PARK → GEAR_DRIVE → GEAR_PARK | 바뀔 때마다 옴 | |

## C. NaviHelper

| 항목 | 결과 | 값 | 비고 |
|---|---|---|---|
| initialize | 가능 | 성공 (약 0.1초) | |
| getCurrentLocationInfo 필드 | 가능 | latitude=37.4127212, longitude=127.094681, heading=0.0, address=수정구 시흥동, routeId=(빈 값), currentRouteIndex=0 | 필드는 이 6개가 전부 |
| 제한속도 | 가능 (값 확인 필요) | 0 | CurrentLocationInfo에는 없음. `onCurrentRoadSpeedLimit(int)` 콜백이 **약 1초마다** 옴. 경로 안내 없이 정차 상태라 0이었음 |

## D. FusedLocation

| 항목 | 결과 | 값 | 비고 |
|---|---|---|---|
| isSupported (initialize 직후 / 1.5초 후) | 가능 | true / true | |

## E. STT·TTS

| 항목 | 결과 | 값 | 비고 |
|---|---|---|---|
| STT initialize | 가능 | onSuccess | Mode.SERVER |
| STT registerApp | 확인 못 함 | 키 없음 | local.properties에 PLEOS_CLIENT_ID/SECRET을 넣은 뒤 다시 확인 |
| TTS initialize | 가능 | onSuccess | Mode.SERVER |
| TTS registerApp | 확인 못 함 | 키 없음 | 위와 같음 |

## F. 오버레이

| 항목 | 결과 | 값 | 비고 |
|---|---|---|---|
| Settings.canDrawOverlays | 가능 | true | `adb shell appops set --user 10 com.tracers.trace SYSTEM_ALERT_WINDOW allow`로 줌 |
| 64dp 칩 (오른쪽 위) | 가능 | frame=[2496,90][2560,154] | 상단바(90px) 바로 아래 오른쪽 끝에 뜸 (`03_overlay_chip.png`) |

## 정리

**사자 카트에 바로 쓸 수 있는 값**
- 속도와 기어: AAOS `PERF_VEHICLE_SPEED`, `GEAR_SELECTION`
- Vehicle SDK의 이벤트형 값:
  - 브레이크 페달 (on/off)
  - 조향각 (부호 주의)
  - 방향지시등
  - 에어컨
  - 와이퍼 (on/off)
  - 총 주행거리
- 위치: NaviHelper `getCurrentLocationInfo` (주소 포함), 제한속도 콜백 (1Hz)
- FusedLocation (지원됨)
- 오버레이 칩

**대체 수단이 필요한 값**
- **연비·연료량:** 에뮬레이터 차가 EV라서 Fuel API가 전부 미지원이다.
  - 에뮬레이터에서는 EvBattery로 대신 보여주거나, 실차(내연기관)에서 Fuel을 쓰는 분기를 둔다.
  - 탱크 용량은 SDK에 아예 없으므로 AAOS `INFO_FUEL_CAPACITY`를 쓴다.
- **점검 (오일·브레이크액·워셔액):** 에뮬레이터에서 전부 미지원이고, 경고등은 권한 오류다.
  - 이대로라면 "점검" 기능은 주행거리 기반 알림(예: N km마다 엔진오일)으로 대체해야 한다.
- **이번 주행 거리·시간:** SDK가 미지원이다. 앱에서 속도를 적분하거나 시작 시점의 총 주행거리와의 차이로 계산한다.
- **안전점수:** 급가속·급감속은 브레이크가 on/off만 오므로, 속도 변화량(100ms 간격)으로 계산하는 게 현실적이다.

**주최 측에 물어볼 것**
1. 실차(또는 심사 환경)는 내연기관인가, EV인가? Fuel API가 실차에서 지원되는가?
2. `getTelltalesInfo`는 어노테이션상 CAR_SAFETY인데 "need Permission"으로 거부된다. 서드파티에 경고등 조회를 열어 줄 수 있는가? 아니면 PRIVATE_CAR_SAFETY가 필요한가?
3. 엔진오일·브레이크액·워셔액 레벨을 에뮬레이터에서 시뮬레이션할 방법이 있는가?
4. 조향각 부호: 콘솔 +90이 SDK에서는 -90이다. 어느 쪽이 실차 기준인가?
5. Trip 계열(이번 주행 거리·시간) 속성은 실차에서 지원되는가?
6. STT·TTS용 Client ID·Secret 발급 방법
7. (기획 검증) 좌석 탑승 `getCurrentSeatOccupancy`가 에뮬레이터에서 콘솔 값과 반대로 온다(Occupied → false). SDK 의미가 "비어 있음=true"인가, 에뮬레이터 매핑 오류인가? 실차에서는 어느 쪽인가?
8. (기획 검증) 안전벨트 착용(SEAT_BELT_BUCKLED)을 가상 콘솔에서 바꿀 방법이 있는가? 지금은 경고등 카드만 있다.
9. (기획 검증) 좌표나 키워드로 주변 장소(충전소·정비소) 이름을 받는 API를 서드파티에 열어 줄 계획이 있는가? NaviHelper에는 내비 화면을 여는 `requestNavigationScreen(SearchPlace/NearbyChargingStations)`만 있다.
10. (기획 검증) `requestRoute`에 poiId만 넣었을 때 내비가 `poiDetails error`를 낸다. `poiProvider`에는 어떤 값을 넣어야 하는가?
11. (기획 검증) `onCautionPointInfo`(SPEED_BUMP 등)는 경로 안내 중에만 오는가, 안전 운전 모드(SAFE_GUIDING)에서도 오는가? 에뮬레이터에서 띄워 볼 방법이 있는가?

## 내연기관 전환 시도

- 일시: 2026-11-02 14:05~14:10 (약 5분. 30분 제한 안에 결론이 남)
- **결론: 안 된다.**
  - 에뮬레이터의 차량 설정(VHAL)에 전기차로 고정돼 있다.
  - Pleos Fuel API가 쓰는 속성은 아예 정의돼 있지 않다.
  - 값을 바꾸는 경로(`car_service` 셸 명령, VHAL 디버그, adb root)는 user 빌드라서 전부 막혀 있다.
  - "값만 바뀌는" 단계까지도 가지 못했다.

### 1. 가상 콘솔 조사 → 관련 엔드포인트 없음

- **받은 파일:** `/`(index.html, 앱 JS가 인라인으로 들어 있음), `/highway.css`, `/vendor/*.js`(three.js 계열), `/vehicle/catalog`, `/vehicle/state`
- **검색어:** fuel, engine, powertrain, ice, vehicle_type, profile, INFO_FUEL, ev_connector, car_type, variant, combustion, gasoline, diesel, hybrid
- **검색 결과:**
  - "engine"은 경고등 이름에만 있다(EngineOilPressure, EngineTemperature, Engine).
  - "ice"는 무드등 색 이름이다.
  - "profile"은 3D 렌더링 품질 설정이다.
  - 연료나 차종과 관련된 키는 없다.
- **catalog의 쓰기 엔드포인트 20개 (숨김 카드 포함):** body, controls, driving-mode, etcs, **ev-battery**, gear, glove-box, hvac, hvac-air-cleaner, lights, notifications, odometer, power-system, reading-lights, seat, steering-wheel-heat, telltales, tire-pressure, turn-signal, window-lock. 연료 관련은 없다.
- **목록에 없는 경로 추측 (GET):** `/vehicle/fuel`, `powertrain`, `profile`, `info`, `vehicle-type`, `engine`, `fuel-level`, `energy`는 전부 `{"error":"not_found"}`를 돌려준다.

```bash
curl -s http://127.0.0.1:8088/ > index.html
curl -s http://127.0.0.1:8088/vehicle/catalog > catalog.json
curl -s http://127.0.0.1:8088/vehicle/state > state.json
curl -s http://127.0.0.1:8088/vehicle/fuel
```

### 2. 차량 설정 확인 → VHAL 설정 파일에 전기차로 고정

- **빌드:** `ro.build.type=user`, `ro.debuggable=0`, `ro.build.tags=test-keys`
- **에뮬레이터 이름:** `ro.boot.qemu.avd_name=Pleos_Connect_v2.1.0`
- **Pleos 서비스:** `init.svc.ai.pleos.hardware.profile=running` 서비스가 돌고 있다. 하지만 이를 조작하는 공개 인터페이스는 찾지 못했다.

`dumpsys car_service`에 나온 config:

| 속성 | ID | access | changeMode | 현재 값 |
|---|---|---|---|---|
| INFO_FUEL_TYPE | 0x11410105 | READ(0x1) | STATIC(0x0) | ELECTRIC (PROBE에서 [10]) |
| INFO_EV_CONNECTOR_TYPE | 0x11410107 | READ | STATIC | IEC_TYPE_1_CCS_DC (설정 파일 기준) |
| FUEL_LEVEL | 0x11600307 | READ | CONTINUOUS(0x2), 1~100Hz | 15000.0 (lastEvent) |
| INFO_FUEL_CAPACITY | 0x11600104 | READ | STATIC | 15000.0 |

**VHAL 설정 파일**

- 위치: `/vendor/etc/automotive/vhalconfig_sdv/*.json`. 누구나 읽을 수 있는 권한이라 adb pull로 복사해서 읽기만 했다. 기기의 파일은 고치지 않았다.
- `VehicleInfo.json`:
  - `INFO_FUEL_TYPE` = `FuelType::FUEL_TYPE_ELECTRIC`
  - `INFO_EV_CONNECTOR_TYPE` = `IEC_TYPE_1_CCS_DC`
  - `INFO_FUEL_CAPACITY` = 15000.0
  - `INFO_EV_BATTERY_CAPACITY` = 77400.0
- `Energy.json`: `FUEL_LEVEL` 기본값 15000.0
- **Pleos Fuel API가 쓰는 vendor 속성은 이 설정 파일들과 car_service 속성 목록(337개) 어디에도 없다:**
  - 0x21400be5 (FUEL_PERCENT)
  - 0x21600be0 (INSTANTANEOUS_FUEL_ECONOMY)
  - 0x21600be1 (SINCE_REFUEL_FUEL_ECONOMY)
  - 0x21600be2 (CUMULATIVE_FUEL_ECONOMY)
  - TRIP_FUEL_ECONOMY는 SDK의 enum에 이름만 있고 ID는 찾지 못했다.
- 기기 쪽에 속성 자체가 없으므로, 값을 넣는 방법이 있었더라도 Fuel API는 여전히 미지원이었을 것이다.

```bash
adb shell getprop | grep -iE 'build.type|debuggable|pleos|avd_name'
adb shell dumpsys car_service | grep -E 'INFO_FUEL_TYPE|INFO_EV_CONNECTOR_TYPE|FUEL_LEVEL|INFO_FUEL_CAPACITY'
adb shell ls -la /vendor/etc/automotive/vhalconfig_sdv
adb pull /vendor/etc/automotive/vhalconfig_sdv ./vhal
```

### 3. 값 주입 시도 → 전부 거부

| 명령 | 결과 |
|---|---|
| `adb shell cmd car_service inject-vhal-event 0x11600307 0 5000` | `SecurityException: The command 'inject-vhal-event' requires non-user build` |
| `adb shell cmd car_service inject-vhal-event 0x21400be5 0 30` | 위와 같음 |
| `adb shell cmd car_service inject-vhal-event 0x21600be0 0 12.5` | 위와 같음 |
| `adb shell cmd car_service set-property-value 0x11600307 0 5000` | `... 'set-property-value' requires non-user build` |
| `adb shell cmd car_service set-property-value 0x11410105 0 1` | 위와 같음 |
| `adb shell cmd car_service get-property-value FUEL_LEVEL` | 읽기 명령도 `requires non-user build` |
| `adb root` | `adbd cannot run as root in production builds` |
| `adb shell dumpsys android.hardware.automotive.vehicle.IVehicle/default --set 0x11600307 -f 5000` | `FAILED_TRANSACTION` (`--help`, `--get`도 같음) |

- **주입 후 PROBE 재확인:**
  - AAOS `FUEL_LEVEL`은 15000.0 그대로다.
  - Fuel API 5개도 그대로 미지원이다(`property ID is not in carPropertyConfig list` 또는 `ERROR_NOT_SUPPORTED_PROPERTY`).
- 지시대로 막힌 지점에서 멈췄다. 이미지를 고치거나 쓰기 가능한 시스템 이미지로 띄우는 우회는 하지 않았다.

### 되돌리는 방법

- 이번 시도에서 바뀐 것은 없다. 주입 명령이 모두 거부됐고, `/vendor` 파일은 읽기만 했다.
- 그래도 초기 상태가 필요하면 **Cold Boot로 충분하다.**
  - VHAL 값은 부팅할 때 `vhalconfig_sdv/*.json`의 기본값으로 다시 채워진다.
  - 가상 콘솔이 바꾼 값(주행거리 12345km 등)도 스냅샷 없이 부팅하면 초기화된다.

```bash
emulator -avd Pleos_Connect_v2.1.0 -no-snapshot-load
```

- 앱 데이터까지 지우려면 AVD Manager의 Wipe Data를 쓴다.

### 그래서 사자 카트는

- 에뮬레이터에서 Fuel API를 시연할 방법은 없다.
- **에뮬레이터용 방안:** 전기차 값(`EvBattery`의 SOC·효율, 콘솔의 `ev-battery`로 바꿀 수 있음)으로 "연비·잔량" 화면을 보여주고, 실차 연료 종류에 따라 Fuel API로 바꿔 쓰는 분기를 둔다.
- **주최 측 질문에 추가:**
  1. 내연기관 프로필의 에뮬레이터 이미지(또는 userdebug 이미지)를 제공하는가?
  2. 심사 차량에서 Fuel 속성(0x21400be5, 0x21600be0~2)이 지원되는가?

## 전기차(EV) 데이터 확인

- 일시: 2026-11-02 14:27 시작, 약 25분 (1시간 제한 안)
- 방법: ProbeActivity 표에 **EV 구역**을 추가했다(`EV·A` = AAOS, `EV·B` = Vehicle SDK, `EV·C` = NaviHelper). Logcat 태그 `PROBE`, 형식은 기존과 같다.
- 가상 콘솔 연결 유지 신호를 켠 상태에서 콘솔 API로 값을 바꾸고 콜백을 확인했다.
- 증거 파일 ([probe-evidence/](probe-evidence/)):
  - `probe_log_ev_sequence.txt` + `console_sequence_ev.sh`: 주 시나리오 로그와 쓴 콘솔 명령
  - `probe_log_ev_charging.txt`: 충전 시나리오 로그
  - 스크린샷: `04_ev_after_sequence.png`, `05_ev_charging.png`
- 지시문의 "4. 에뮬레이터에서 실제 동작 확인"이 "연결 유지 신호를 다시 켜고"에서 끊겨 있었다. 그래서 앞선 PROBE와 같은 방식(콘솔로 값 바꾸기 → 콜백 확인 → 이 문서에 기록)으로 진행했다.

### 1. SDK에서 직접 확인 (javap)

#### EvBattery

권한은 `pleos.car.permission.CAR_ENERGY`이고, 용량만 `CAR_INFO`다.

| 메서드 | 반환 타입 | 내부 속성 (EvBatteryImpl 바이트코드) | 판단 |
|---|---|---|---|
| getInstantaneousEvEfficiency | Float | INSTANTANEOUS_EV_EFFICIENCY | **순간 전비** |
| getCurrentEvEfficiency | Float | TRIP_EV_EFFICIENCY + TripStatisticType.CURRENT | **이번 주행 평균** |
| getSinceChargeEvEfficiency | Float | SINCE_CHARGE_EV_EFFICIENCY | **충전 후 평균** |
| getCumulativeEvEfficiency | Float | CUMULATIVE_EV_EFFICIENCY | **누적 평균** |
| getEvBatteryStateOfCharge | Int | EV_BATTERY_SOC | 배터리 % |
| getEvBatteryLevel | Float | EV_BATTERY_LEVEL | 배터리 잔량 (실측 77400 → Wh) |
| getEvBatteryCapacity | Float | INFO_EV_BATTERY_CAPACITY | 배터리 용량 (Wh) |
| isEvChargePortOpen / setEvChargePortOpen | Boolean | EV_CHARGE_PORT_OPEN | 충전구 열림 |
| getEvChargeSwitchState / setEvChargeSwitch | Boolean | EV_CHARGE_SWITCH | 충전 시작·중지 스위치 |
| get/setEvChargeCurrentDrawLimit | Float | EV_CHARGE_CURRENT_DRAW_LIMIT | 충전 전류 제한 (A) |
| setEvChargePercentLimit | (설정만) | EV_CHARGE_PERCENT_LIMIT | 충전 목표 % |

- **SDK에 없는 것:** 남은 주행가능거리, 충전 중 상태, 급속/완속 방식, 충전 속도, 완료 예상 시간, 배터리 온도
  - `EvChargeState`, `EvConnectorType`, `EvRegenerativeBrakingState` enum은 들어 있지만, 이것을 쓰는 공개 메서드는 없다.
  - 남은 주행가능거리는 **Odometer.getRangeRemainingDistance**(Float, CAR_DRIVE, 리스너 `RangeRemainingListener`)에 있다.
- **단위:** AAR에 문서 주석이 없다. 실측과 AOSP 문서로 판단했다(Wh, m, A). 전비 단위(km/kWh 등)는 값이 오지 않아 확인하지 못했다.

#### Tire

| 메서드 | 내부 속성 | 권한 | 실제 의미 |
|---|---|---|---|
| getLowTirePressure(WheelArea) | CRITICALLY_LOW_TIRE_PRESSURE | CAR_SAFETY | 저압 **기준값**(kPa)이다. 경고 여부가 아니다. 실측 137.0 |
| getTirePressure(WheelArea) | TIRE_PRESSURE | **PRIVATE_CAR_SAFETY** | 현재 공기압. 서드파티는 권한 오류 |

- 타이어 온도와 경고 API는 없다.

#### DrivingMode

권한은 `CAR_DRIVE`다.

| 메서드 | 반환 | 내부 속성 |
|---|---|---|
| getCurrentDrivingModeState | DrivingModeType (NORMAL/ECO/SPORT) | DRIVING_MODE |
| getCruiseControlState | CruiseControl (OTHER/ENABLED/ACTIVATED/USER_OVERRIDE/SUSPENDED/FORCED_DEACTIVATION_WARNING) | CRUISE_CONTROL_STATE |
| getCruiseControlTargetSpeed | Float (m/s) | CRUISE_CONTROL_TARGET_SPEED |

- 회생제동 단계와 패들 API는 SDK에 없다. AAOS `EV_BRAKE_REGENERATION_LEVEL`과 `EV_REGENERATIVE_BRAKING_STATE`로 확인했다.

#### HVAC (겨울철 주행거리 감소 설명용)

- 읽기 권한은 CAR_CLIMATE다. 영역은 capability 결과를 그대로 쓴다.
- 읽을 수 있는 값:
  - 공조 전원 `getHvacPowerState`
  - 풍량 `getHvacFanSpeed` (1~8)
  - 설정온도 `getHvacTemperature(area, CELSIUS)` (15~30)
  - 실내 현재온도 `getHvacTemperatureCurrent`
  - 핸들 열선 `SteeringWheel.getHvacSteeringWheelHeatState` (0~3)
  - 시트 열선·통풍 `Seat.getHvacSeatTemperature`
- **히터 전용 API는 없다.** 설정온도와 실외온도를 비교해서 판단해야 한다.

#### NaviHelper

권한은 `NAVI_ROUTE`다.

| 메서드 → 콜백 | 주는 것 | 조건 |
|---|---|---|
| getChargerOperatorInfo() → onChargerOperatorInfo(List<ChargerOperator>) | **충전 사업자** id, 이름만 | 없음. 실측 35개 (HD/현대자동차(E-pit), ME/환경부, KP/한국전력공사 등) |
| onDrivingInfo(DrivingInfo) → DrivingState (목적지·경유지마다) | name, duration, distance, **chargerCount, maxChargingPower, targetSoc, remainDestinationChargingValue, chargingPlan(duration, startSoc, endSoc)** | **경로 안내 중일 때만** 옴. 이번엔 안내 중이 아니라 오지 않음 |
| getRouteStateInfo() → onRouteStateInfo | remainDestinationChargingInt, remainWaypointChargingInt, naviMode, routeOption, trafficStatus | 없음. 실측: 안내 없음 상태라 0 |
| requestTrip(TripRequest) → onTripInfo | distance, eta, **estimatedSocAtDest**, energyConsumption | TripRequest(출발·도착 지정)가 필요함. 이번엔 호출 안 함 |

- `RoutePointPoiType`에 `EV_CHARGING`이 있어서, 경로 지점이 충전소인지는 구분할 수 있다.
- **충전소 이름, 충전기 상태(사용 가능/사용 중), 실시간 빈자리를 주는 API는 없다.**

### 2. AAOS 표준 속성

- 12개 모두 VHAL에 정의돼 있다.
- 매니페스트에 `android.car.permission.CAR_ENERGY_PORTS`와 `CAR_EXTERIOR_ENVIRONMENT`를 추가했고(PROBE용 주석), 전부 허용됐다. 권한 오류는 없었다.

| 속성 | 결과 | access / change | 초기값 | 콘솔로 바꿔 봄 | 콜백 |
|---|---|---|---|---|---|
| EV_BATTERY_LEVEL | 가능 | READ / CONTINUOUS 1~100Hz | 77400 Wh | battery_level=50 → **50000 Wh** | 1초 간격 |
| INFO_EV_BATTERY_CAPACITY | 가능 | READ / STATIC | 77400 Wh | - | - |
| EV_CURRENT_BATTERY_CAPACITY | 가능 (고정) | READ / ONCHANGE | 77400 Wh | 바뀌지 않음 | - |
| EV_CHARGE_PORT_CONNECTED | 가능 (고정) | READ / ONCHANGE | false | 충전을 켜도 false | - |
| EV_CHARGE_PORT_OPEN | 가능 | READ_WRITE / ONCHANGE | false | charge_port_open → **true / false** | 바뀔 때 |
| EV_CHARGE_STATE | 가능 (고정) | READ / ONCHANGE | 2 (FULLY_CHARGED) | SOC 50 + 충전 켜도 **2 그대로** | - |
| EV_BATTERY_INSTANTANEOUS_CHARGE_RATE | 가능 (고정) | READ / CONTINUOUS 1~10Hz | 0 mW | 충전 켜도 0 | 1초 간격 |
| EV_CHARGE_TIME_REMAINING | 가능 (고정) | READ / CONTINUOUS 1~10Hz | 0 s | 충전 켜도 0 | 1초 간격 |
| RANGE_REMAINING | 가능 | READ_WRITE / CONTINUOUS 1~2Hz | 0 m | remaining_range_m=250000 → **250000 m** | 1초 간격 |
| EV_REGENERATIVE_BRAKING_STATE | 가능 (고정) | READ / ONCHANGE | 2 (PARTIALLY_ENABLED) | 콘솔에 항목 없음 | - |
| EV_BRAKE_REGENERATION_LEVEL | 가능 (고정) | READ_WRITE / ONCHANGE | 0 | 콘솔에 항목 없음 | - |
| ENV_OUTSIDE_TEMPERATURE | 가능 (고정) | READ / CONTINUOUS 1~2Hz | 25.0 °C | 콘솔에 항목 없음 | 1초 간격 |

### 3. Vehicle SDK·NaviHelper 실측

| 항목 | 결과 | 값 변화 | 비고 |
|---|---|---|---|
| 순간 / 이번 주행 / 충전 후 / 누적 전비 | **에뮬레이터 미지원** | - | 0x21600b13, b14, b15 not in carPropertyConfig. Current는 ERROR_NOT_SUPPORTED_PROPERTY. Fuel 연비와 같은 상황 |
| 배터리 % (EV_BATTERY_SOC) | 가능 | 100 → 80 → 50 → 100 | 콘솔 soc를 바로 따라감 |
| 배터리 잔량 (EV_BATTERY_LEVEL) | 가능 | 77400 → 50000 | 약 4초마다 반복해서 옴 |
| 배터리 용량 | 가능 | 77400 | |
| 충전구 열림 | 가능 | false → true → false | 콜백이 바로 옴 |
| 충전 스위치 | 가능 (조회만) | false → true | 리스너가 없어 다시 읽어야 보임 |
| 충전 전류 제한 | 가능 (조회만) | 12.5 → 32.0 | 위와 같음 |
| 남은 주행가능거리 (Odometer) | 가능 | 0 → 250000 | 약 4초마다 반복해서 옴 |
| 타이어 저압 기준 | 가능 | 4바퀴 모두 137.0 | 경고 신호가 아니라 기준값 |
| 타이어 공기압 | **권한 오류** | - | "getTirePressure need Permission" (PRIVATE_CAR_SAFETY). 콘솔 FL=180kPa도 확인 불가 |
| 드라이브 모드 | **에뮬레이터 미지원** | - | 0x21400410 not in carPropertyConfig |
| 크루즈 상태 | 가능 | ENABLED → null | 콘솔 not_ready는 SDK enum에 없어서 null이 됨 |
| 크루즈 목표속도 | 가능 | 13.89 → 22.22 m/s | |
| 공조 전원 / 풍량 / 설정온도 | 가능 (조회만) | false→true, 3→6, 22→28 °C | 다시 읽기로 확인 |
| 실내 현재온도 | 가능 (고정) | 19.1 °C | |
| 충전 사업자 목록 | 가능 | 35개 | |
| 경로 충전정보 (onDrivingInfo) | 확인 못 함 | - | 경로 안내 중이 아니라 안 옴 |
| 경로 상태 (getRouteStateInfo) | 가능 | 남은 충전값 0 | |

### 주행 흉내 확인 (추가, 2026-11-02 14:42~14:55)

**1. D + 60km/h로 3분 → 저절로 변하지 않는다**

- 1초 간격으로 콘솔 상태(`/vehicle/state`)를 180번 찍었다. 같은 시간 앱의 SDK·AAOS 값은 PROBE 로그로 확인했다.
- 3분 동안 다음 값이 **한 번도 변하지 않았다.** 콘솔과 앱 양쪽 모두 같다.
  - 배터리 % 100
  - 잔량 50 kWh (= 50000 Wh)
  - 주행가능거리 250000 m
  - 총 주행거리 12345 km
  - 주행시간 42분
- 증거: `probe-evidence/ev_drive_sampling.csv`, `probe_log_ev_drive.txt`

**2. 브레이크·감속·공조 → 변하지 않는다**

| 단계 (각 10~15초) | 속도 | 배터리·잔량·주행가능거리·총 주행거리 |
|---|---|---|
| 브레이크 100% (speed 값은 60 유지) | **60 그대로**. 콘솔은 감속도 흉내 내지 않음 | 변화 없음 |
| 속도 20 + 브레이크 30% | 20 | 변화 없음 (회생제동 충전 없음) |
| 공조 켬 (28도, 풍량 8, AC) | 20 | 변화 없음 (공조 소비 없음) |
| 공조 끔 | 20 | 변화 없음 |

- **결론:** 에뮬레이터는 주행을 흉내 내지 않는다. 속도, 주행거리, 배터리는 서로 독립된 값이고, **콘솔로 직접 바꿔야만 변한다.**

**3. 시나리오 재생 스크립트 → 가능하고, 동작까지 확인했다**

- 파일:
  - `tools/scenario/replay.py`: Python 표준 라이브러리만 쓴다.
  - `tools/scenario/sample_30s.json`
- 하는 일:
  - JSON의 `steps`를 시간 순서대로 가상 콘솔 API로 보낸다(기어, 속도·페달·조향, 총 주행거리, 주행가능거리, 배터리 kWh·SOC, 공조, 방향지시등).
  - 콘솔 연결 유지 신호를 스스로 보낸다.
- `auto` 블록을 쓰면 매초 속도로 주행거리를 적분해 같이 반영한다.
  - 배터리 소비 = 전비(`km_per_kwh`) + 공조 전력(`hvac_kw`)
  - 주행가능거리 = 남은 에너지 × 전비
  - `speedup`을 쓰면 짧은 시연에서 거리·소비만 빨리 감을 수 있다.
- 앱은 아무것도 바꾸지 않고 평소처럼 SDK로 값을 받는다.
- 실행:

```bash
adb forward tcp:8088 tcp:8088
python3 tools/scenario/replay.py tools/scenario/sample_30s.json --adb-mark
```

- **30초 샘플 결과:** 20배속, 전비 6 km/kWh, 공조 1.5 kW
  - 총 주행거리: 12345 → 12350.859 km. `getCurrentDistanceDriven`에서 소수점까지 받음
  - 배터리 잔량: 50000 → 48879 Wh. `getEvBatteryLevel`와 AAOS `EV_BATTERY_LEVEL` 모두 받음
  - SOC: 65 → 64 → 63
  - 주행가능거리: 300000 → 293274 m. `getRangeRemainingDistance`와 `RANGE_REMAINING` 모두 받음
  - 속도·기어·방향지시등은 시나리오 시각대로 바뀜
  - 공조를 켠 10~20초 구간은 같은 60km/h인데도 초당 Wh 감소가 56 → 65로 커져서, 공조 소비가 보인다.
  - 증거: `probe-evidence/scenario_30s_output.txt`, `probe_log_scenario_30s.txt`
- **주의할 점:**
  - `--adb-mark`의 STEP 표시는 adb 호출 때문에 실제 변경보다 약 1초 늦게 찍힌다.
  - SDK 쪽 잔량과 주행거리 콜백은 약 1초, AAOS 쪽은 1초 간격이라 1초보다 촘촘한 변화는 의미가 없다.
  - 전비 4종(Instantaneous 등)은 여전히 미지원이다. 앱이 잔량 감소량 ÷ 거리 증가량으로 계산해야 한다.
  - 충전 상태, 충전 속도, 드라이브 모드는 콘솔로도 바꿀 수 없어서 시나리오에 넣을 수 없다.

**시연 방식 추천**

1. **출퇴근 15분 시나리오 JSON**(`speedup` 1, 실제 시간)을 만들어 두고, 시연 직전에 `replay.py`로 재생한다.
   - 심사 영상은 3~5분 버전(`speedup` 3~5)을 따로 둔다.
2. 앱은 SDK·AAOS로만 값을 받는다. 시연용 분기나 가짜 데이터를 넣지 않는다.
   - 실차에서도 코드가 그대로라는 점을 설명할 수 있다.
3. 전비, 이번 주행 거리·시간은 앱이 직접 계산한다.
   - 잔량(Wh) 변화량, 총 주행거리 변화량, 주행 시작 시각을 기준으로 한다.
   - 실차에서 SDK 전비가 지원되면 그 값으로 바꿔 쓴다.
4. 겨울철 설명은 시나리오에 "공조 켬 + 높은 설정온도" 구간을 넣는다. 그러면 같은 속도에서 Wh 감소가 커지는 것으로 보여줄 수 있다.
   - 실외온도는 콘솔로 바꿀 수 없어서 25°C로 고정이다.

### 정리 (사자 카트 연료 기능 → 전기차)

**바로 쓸 수 있는 것**
- 배터리 %: `getEvBatteryStateOfCharge`
- 배터리 잔량·용량 (Wh)
- 남은 주행가능거리: `RANGE_REMAINING` 또는 `getRangeRemainingDistance`
- 충전구 열림
- 공조 상태와 설정온도, 실외온도 (겨울철 설명용)
- 충전 사업자 목록

**에뮬레이터에서 시연할 수 없는 것**
- 전비 4종: Fuel 연비처럼 vendor 속성이 VHAL에 없다.
  - **대체:** 배터리 잔량(Wh)의 감소량을 주행거리 증가량으로 나눠 앱에서 직접 계산한다.
- 충전 진행 (상태, 속도, 남은 시간): 콘솔에서 충전을 켜도 차 쪽 값이 바뀌지 않는다.
- 드라이브 모드: 미지원이다.
- 회생제동 단계, 실외온도: 값은 읽히지만 콘솔로 바꿀 수 없다.

**서드파티 권한으로 막힌 것**
- 타이어 공기압 (PRIVATE_CAR_SAFETY)

**주최 측에 물어볼 것**
1. 실차에서 EV 전비 속성(0x21600b13~15, TRIP_EV_EFFICIENCY)이 지원되는가? 단위는 무엇인가?
2. 에뮬레이터에서 충전 시뮬레이션(EV_CHARGE_STATE·충전 속도·남은 시간)을 할 방법이 있는가?
3. 충전소 상태(사용 가능 여부)를 받을 수 있는 API가 있는가? NaviHelper는 사업자 목록과 경로상 충전 계획만 준다.
4. 타이어 공기압을 서드파티에 열어 줄 수 있는가?

## 기획 검증 (장소 이름·흔들림·좌석)

2026-11-02 19:05~19:35. 에뮬레이터 Cold Boot 후 진행, 커밋 안 함.
- API 이름은 전부 SDK jar를 javap로 열어 확인했다: NaviHelper 3.0.5, navi-data 3.0.4, Vehicle 2.3.2, vehicle-data 1.1.11.
- PROBE 화면에 "기획1·", "기획2·", "기획3·" 줄을 추가했다.
  - `ServiceProbe.kt`: 장소 이름, 주의 지점, Geocoder
  - `SensorProbe.kt` (새 파일): 센서
  - `AaosProbe.kt`, `VehicleProbe.kt`: 좌석
- 버튼 "기획1 목적지 시험"과 "경로 취소"를 추가했다.
- 증거 파일 (`docs/probe-evidence/`):
  - `probe_log_plan_start.txt`
  - `probe_log_plan_seat_toggle.txt`
  - `probe_log_plan_route.txt`
  - `06_plan_route_navi.png`

### 1. 장소 이름 (충전소·정비소)

**a. NaviHelper에서 name·poi·place·search·destination·nearby·charger·station이 들어간 것 (javap)**

| 종류 | 이름 | 장소 이름 필드 | 권한 |
|---|---|---|---|
| 메서드 | `getDestinationInfo()` → `onDestinationInfo(DestinationInfo)` | `name`, `address`, `poiId`, `poiSubId`, `poiProvider` | NAVI_ROUTE |
| 메서드 | `getWaypointInfo()` → `onWayPointInfo(List<WaypointInfo>)` | `name`, `address`, `poiId` | NAVI_ROUTE |
| 메서드 | `getRecentDestinationInfo()` → `onRecentDestinationInfo(List<RecentDestinationInfo>)` | `name`, `address`, `poiId` | NAVI_ROUTE |
| 메서드 | `getBookmarkInfo()` → `onBookmarkInfo(List<BookmarkInfo>)` | `name`, `address`, `category` (HOME / OFFICE / EV_CHARGING_STATION / DEFAULT) | NAVI_ROUTE |
| 메서드 | `getChargerOperatorInfo()` → `onChargerOperatorInfo(List<ChargerOperator>)` | `name` (사업자 이름이고 충전소 이름이 아님) | NAVI_ROUTE |
| 메서드 | `requestRoute(RouteInfo)`, `requestRouteWithWaypoints`, `addWaypoint(RequestWaypointInfo)` | 입력: `poiName`, `poiId`, `address`, `poiProvider` | NAVI_ROUTE_SEARCH |
| 메서드 | `requestTrip(TripRequest)` → `onTripInfo(TripInfo)` | `summary.routePoints[].poiName`, `poiType` (NORMAL / EV_CHARGING) | NAVI_ROUTE |
| 메서드 | `requestNavigationScreen(SearchPlace(keyword) / NearbyChargingStations / POIDetailPopup(poiId, provider))` | 내비 화면만 연다. 결과를 돌려주지 않는다 | NAVI_CUSTOM_MAP |
| 콜백 | `onRouteStarted(RouteStartInfo)` | `name` | - |
| 콜백 | `onDestinationArrived(DestinationArrivedInfo)`, `onWayPointArrived(WaypointArrivedInfo)` | `name`, `address` | - |
| 콜백 | `onDrivingInfo(DrivingInfo)` → `DrivingState` | `name`, `isChargingStation`, `isRecommendedChargingStation`, `chargerCount`, `maxChargingPower` | - |
| 없음 | `CurrentLocationInfo` | `address`만 있다 (`latitude`, `longitude`, `heading`, `routeId`, `currentRouteIndex`) | - |

- **좌표로 주변을 검색하는 메서드는 없다.** search·nearby가 들어간 것은 `NavigationScreenRequest$SearchPlace`와 `$NearbyChargingStations`뿐이다. 둘 다 화면 전환용 요청 객체다.
- `getInternalApi()`가 반환하는 `InternalApi`는 메서드가 없는 빈 인터페이스다.

**b. 실제 값 (에뮬레이터)**

| 항목 | 결과 | 판정 |
|---|---|---|
| `getRecentDestinationInfo` | 1개: `이케아팝업스토어 현대백화점판교점 / 경기도 성남시 분당구 판교역로146번길 20 / poi=36khde3d9dnscm5nqher`. 사용자가 내비에서 직접 고른 장소는 이름이 온다 | 가능 |
| `getBookmarkInfo` | 2개: HOME, OFFICE. 이름과 주소가 비어 있다 (등록 안 됨) | 가능 (등록돼 있으면) |
| `requestRoute`, 좌표만 (이름 비움) | `onRouteStarted name=""`, distance=1900. `getDestinationInfo`는 `name=""`, `address="수정구 시흥동"`. **내비는 좌표로 주소만 채우고 장소 이름은 채우지 않는다** | 불가 |
| `requestRoute`, poiId만 (이름 비움) | 내비 로그에 `NavigationUseCase::poiDetails error`, 경로가 시작되지 않음 (poiProvider 비움) | 실차 확인 필요 |
| `requestTrip`, 부산역 좌표 | distance=378751m, estimatedSocAtDest=29, routePoints=`DESTINATION/NORMAL/MANUAL name=""`. 충전 경유지(EV_CHARGING)는 붙지 않았다 | 이름 없음 |
| `onDrivingInfo` (경로 안내 중) | `destination.name=""`, `isChargingStation`·`chargerCount=0` | 이름 없음 (요청한 대로 돌아옴) |
| `requestRoute` 권한 | 처음에는 "권한 오류: NAVI_ROUTE_SEARCH is required"가 났다. normal 권한이라 매니페스트에 추가하니 해결됐다 | 가능 |
| 부작용 | `requestRoute`를 호출하면 **내비 앱이 앞 화면으로 올라온다** | 주의 |

**c. 좌표로 주변 장소 검색**: 해당 메서드가 없다 → **불가**

**d. Geocoder**: `Geocoder.isPresent()=false`. `getFromLocation(37.4127, 127.0947, 5)`는 결과 0개. 에뮬레이터에 지오코딩 백엔드가 없다 → **불가** (실차에 Google 서비스가 있는지는 실차 확인 필요)

**결론**
- 장소 이름은 사용자가 내비에서 직접 고른 목적지, 최근 목적지, 즐겨찾기, 경유지에서만 온다.
- 좌표만으로 이름을 얻는 방법은 없다.
- **기획안의 대체 방법(주소 + 사용자가 붙이는 이름)을 유지한다.**
- 개선점: "최근 목적지·즐겨찾기에서 고르기"를 함께 넣으면, 사용자가 직접 입력하지 않아도 내비 POI 이름을 쓸 수 있다.

### 2. 위아래 흔들림 (과속방지턱)

**a. 센서 목록** (`getSensorList(TYPE_ALL)`, 8개. 모두 에뮬레이터 가상 센서)

| 이름 | 제조사 | 종류 |
|---|---|---|
| Goldfish Ambient Temperature sensor | The Android Open Source Project | ambient_temperature (13) |
| Goldfish Light sensor | 〃 | light (5) |
| Goldfish Humidity sensor | 〃 | relative_humidity (12) |
| Goldfish 3-axis Magnetic field sensor (uncalibrated) | 〃 | magnetic_field_uncalibrated (14) |
| Goldfish 3-axis Gyroscope (uncalibrated) | 〃 | gyroscope_uncalibrated (16) |
| Goldfish 3-axis Accelerometer Uncalibrated | 〃 | accelerometer_uncalibrated (35) |
| Accelerometer Limited Axes Uncalibrated Sensor | AOSP | accelerometer_limited_axes_uncalibrated (40) |
| Gyroscope Limited Axes Uncalibrated Sensor | AOSP | gyroscope_limited_axes_uncalibrated (41) |

- `getDefaultSensor(TYPE_ACCELEROMETER)`는 **null**이다. 보정된 가속도 센서가 없어서 1초 샘플은 하지 않았다.
- 미보정 가속도(type 35)는 있다. 하지만 Goldfish 가상 센서라서 가상 콘솔의 차량 움직임과 연결되지 않는다.
- 판정: **에뮬레이터만 가능 / 실차 확인 필요.** 실차 헤드유닛에 가속도 센서가 있는지는 알 수 없다.

**b. 차량 속성·SDK**
- `dumpsys car_service` 속성 목록에서 ACCEL, IMU, G_SENSOR, YAW, PITCH, VERTICAL, BUMP를 찾았다. 걸린 것은 `SEAT_LUMBAR_VERTICAL_MOVE/POS`(좌석 허리받침)뿐이다.
- Vehicle SDK에서 걸린 것은 다음 세 가지뿐이다:
  - `TelltaleType.ACCEL_RESPONSE_*` (경고등)
  - `PleosPropertyIds.DRIVE_ACCELERATION_LEVEL` (가속 응답 설정)
  - `SEAT_LUMBAR_VERTICAL_*`
- 차량 가속도, IMU, 피치는 **불가**.

**c. NaviHelper**
- `onCautionPointInfo(List<CautionPoint>)`가 있다.
  - `CautionPoint`의 필드: `id`, `type: CautionPointType`, `remainDistance` (m)
  - `CautionPointType`의 첫 값이 **`SPEED_BUMP`**다. 그 밖에 SCHOOL_ZONE, ROAD_DAMAGE_ALERT, SLIPPERY_ROAD_ALERT, NARROW_ROAD, DOWNHILL_CAUTION_AREA 등 23종이 있다.
- `onCameraAlert(List<CameraAlert>)`가 있다 (단속카메라 종류, 제한속도, 남은 거리, 과속 여부).
- 에뮬레이터에서는 정차 상태로 경로 안내(ROUTE_DRIVE)를 해 봤지만 한 번도 오지 않았다 → **실차 확인 필요** (API는 있음).

**결론**
- 센서로 흔들림을 직접 재는 방법은 없다.
- 대신 **내비의 `CautionPointType.SPEED_BUMP` + `remainDistance`가 생겼다.** "앞 N m 과속방지턱"이 오고, 그 지점을 지나는 동안의 속도 변화를 보면 "방지턱 앞에서 잘 감속했는지"를 판정할 수 있다.
- **속도 패턴 추정은 유지하고, 내비 방지턱 안내로 보강하는 쪽이 낫다.** 실차에서 콜백이 오는지는 꼭 확인해야 한다.

### 3. 좌석 탑승 여부

**a. Vehicle SDK (javap, `@RequiresPermission`)**

| API | 클래스 | 권한 | 실측 |
|---|---|---|---|
| `checkSeatOccupancyCapabilities`, `getCurrentSeatOccupancy(SeatArea)`, `register/unregisterSeatOccupancy(SeatArea, SeatOccupancyListener)` | Seat | pleos CAR_SEATS | access=READ. 영역 5개: ROW_1_LEFT, ROW_1_RIGHT, ROW_2_LEFT, ROW_2_CENTER, ROW_2_RIGHT. 콜백 옴 (**값이 반대**, 아래 c) |
| `checkSeatBeltBuckledCapabilities`, `getCurrentSeatBeltBuckledState(SeatArea)`, `register/unregisterSeatBeltBuckledState` | Seat | pleos CAR_SEATS | access=READ_WRITE. 영역 5개, 전부 `false`. 화면에는 "안전 벨트를 매세요"가 뜬다 |
| `checkSeatBeltHeight*`, `getCurrentSeatBeltHeightPosition`, `moveSeatBeltHeight` | Seat | CAR_SEATS / CONTROL_CAR_SEATS | (벨트 높이, 기획과 무관) |
| `checkSeatBuckledCapabilities`, `checkSeatNotBuckledWarningCapabilities`, `hasSeatBeltNotBuckledWarning`, `register/unregisterSeatBeltNotBuckledWarning` | Safety | pleos CAR_SAFETY | `ERROR_NOT_SUPPORTED_PROPERTY` |

- "passenger"가 들어간 것은 경고등 `PASSENGER_AIRBAG_ON/OFF`와 `PleosPropertyIds.PASSENGER_SEAT_AIRBAG_OFF`뿐이다.

**b. AAOS 표준 속성**

| 속성 | VHAL (dumpsys) | 앱에서 | 판정 |
|---|---|---|---|
| `SEAT_OCCUPANCY` 0x15400bb0 | access READ, ON_CHANGE. areaId 0x1 (1열 왼쪽), 0x4 (1열 오른쪽), 0x10 (2열 왼쪽), 0x40 (2열 오른쪽) | `config=null` | 권한 오류 |
| `SEAT_BELT_BUCKLED` 0x15200b82 | access READ_WRITE, ON_CHANGE. areaId는 위와 같음 | `config=null` | 권한 오류 |

- 읽으려면 `android.car.permission.CONTROL_CAR_SEATS`가 필요하다. 이 권한은 `prot=signature|privileged`라서 서드파티 앱에는 거부된다.
- 매니페스트에 추가는 했다 ("PROBE용, 출시 전 정리" 주석).
- Pleos SDK가 2열 가운데(ROW_2_CENTER)를 하나 더 보고하는데, VHAL에는 이 영역이 없다.

**c. 가상 콘솔**
- **Seat Occupancy 카드**: `PUT /vehicle/seat?kind=occupied&area=driver|passenger|rear_left|rear_right&value=true|false`
- 안전벨트 착용 카드나 API는 **없다.** `/vehicle/telltales/simulator`의 SeatBelt 경고등만 있고, 이것은 SEAT_BELT_BUCKLED 값이 아니다.
- 콘솔에서 탑승 값을 바꿔 가며 SDK 콜백을 확인했다. 3초 간격, `probe_log_plan_seat_toggle.txt`.

| 콘솔 | SDK 콜백 `onSeatOccupancyUpdated` |
|---|---|
| driver=true (Occupied) | ROW_1_LEFT **false** |
| passenger=true | ROW_1_RIGHT **false** |
| driver=false (Empty) | ROW_1_LEFT **true** |
| rear_left=true | ROW_2_LEFT **false** |
| rear_right=false (이미 false) | 콜백 없음 |
| passenger=false | ROW_1_RIGHT **true** |

- 콜백은 바로 오지만 **값이 콘솔과 반대**다. 처음 상태도 콘솔은 전부 Empty인데 SDK는 전부 `true`였다.
- AAOS 원값(1=VACANT, 2=OCCUPIED)을 SDK가 Boolean으로 바꾸는 과정의 문제로 보이지만, AAOS 쪽이 권한 오류라 원값을 직접 확인할 수 없다.
- 판정:
  - 탑승: **에뮬레이터만 가능** (값 의미는 실차 확인 필요)
  - 벨트: 읽기는 되지만 에뮬레이터에서 바꿀 수 없다 → **실차 확인 필요**

**결론**
- **직접 선택보다 나은 방법이 생겼다.** Pleos SDK `Seat.getCurrentSeatOccupancy` / `registerSeatOccupancy` (서드파티 권한 pleos CAR_SEATS)로 좌석별 탑승을 받을 수 있다.
- 다만 에뮬레이터에서 true/false 의미가 반대라서 실차 확인 전까지는 다음처럼 한다:
  - 값의 의미를 설정에서 뒤집을 수 있게 둔다.
  - **직접 선택을 대체 수단으로 남겨 둔다.**

### 세 가지 한 줄 결론

| 기획 | 결론 |
|---|---|
| 장소 이름 | **대체 방법 유지** (주소 + 사용자 이름). 좌표로 이름을 얻는 API가 없다. 최근 목적지·즐겨찾기에서 고르는 방법은 추가할 수 있다 |
| 위아래 흔들림 | **더 나은 보조 수단 생김**: 내비 `onCautionPointInfo`의 `SPEED_BUMP` + 남은 거리. 속도 패턴 추정은 유지하고, 방지턱 위치 정보로 보강한다 (실차 확인 필요) |
| 좌석 탑승 | **더 나은 방법 생김**: SDK `Seat.registerSeatOccupancy` (좌석별 콜백). 값이 반대로 오는 문제 때문에 직접 선택은 대체 수단으로 유지한다 |

주최 측 질문 7~11을 위 "정리 → 주최 측에 물어볼 것" 목록에 추가했다.
