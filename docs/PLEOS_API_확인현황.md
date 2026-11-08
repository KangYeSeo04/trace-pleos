# Pleos API 확인 현황

## 확인된 것 (핵심 기능에 사용)

| 항목 | API | 비고 |
|------|-----|------|
| 차량 속도 | AAOS `PERF_VEHICLE_SPEED` | 중앙값 100ms 간격 |
| 기어 | AAOS `GEAR_SELECTION` 또는 Vehicle SDK `Gear` | CURRENT_GEAR는 불가 |
| 브레이크 | Vehicle SDK `Brake.getCurrentBrakePedalStatus` | **Boolean on/off만** (밟은 정도 % 아님) |
| 조향각 | Vehicle SDK `SteeringWheel.getCurrentSteeringWheelAngle` | 약 3.1초마다 + 변경 시. **콘솔과 부호 반대** (콘솔 +90 → SDK -90) |
| 방향지시등 | Vehicle SDK `TurnSignal` | OFF/LEFT/RIGHT/HAZARD |
| 위치 | NaviHelper `getCurrentLocationInfo` | 위도, 경도, heading, 주소 포함 |
| 제한속도 | NaviHelper `onCurrentRoadSpeedLimit` | 1초마다 콜백 |
| 음성 | Gleo AI STT·TTS | initialize 성공, **registerApp에 Client ID/Secret 필요** |
| EV 배터리 % | Vehicle SDK `EvBattery.getEvBatteryStateOfCharge` | 0~100 |
| EV 배터리 잔량 | Vehicle SDK `EvBattery.getEvBatteryLevel` 또는 AAOS `EV_BATTERY_LEVEL` | Wh 단위 |
| EV 배터리 용량 | Vehicle SDK `EvBattery.getEvBatteryCapacity` | Wh 단위 |
| 남은 주행가능거리 | Vehicle SDK `Odometer.getRangeRemainingDistance` 또는 AAOS `RANGE_REMAINING` | m 단위 |
| 충전구 열림 | Vehicle SDK `EvBattery.isEvChargePortOpen` | |
| 총 주행거리 | Vehicle SDK `Odometer.getCurrentDistanceDriven` | km 단위 (AAOS PERF_ODOMETER는 권한 오류) |
| 에어컨 | Vehicle SDK `HVAC.getHvacAcState` | |
| 와이퍼 | Vehicle SDK `Wiper.getCurrentWiperState` | ON/OFF (단계는 안 나옴) |
| 서드파티 앱은 AAOS 차량 속성을 읽기만 가능(쓰기 불가) | | |

## 에뮬레이터 실측 결과 (2026.11.02)

에뮬레이터: Pleos Connect v2.1.0, **차량이 전기차(EV)로 고정**되어 있음. 상세: `docs/probe-result.md`, `probe-results/RESULTS.md`

### 화면 크기 (2026.11.08 측정)

| 항목 | 값 |
|------|-----|
| 에뮬레이터 전체 | 2560 × 1440 px |
| 밀도 | 160 dpi (mdpi, 1x) |
| 3분할 TRACE 칸 | 841 × 1210 dp |
| 디자인 기준 | 600 × 800 dp |
| **scale** | min(841/600, 1210/800) = **1.40** |

- scale은 실행 시 칸 크기로 동적 계산: `min(칸가로/600, 칸세로/800)`
- TraceTheme에서 LocalDensity를 `density × scale`로 덮어써서, 화면 코드는 디자인 px 숫자를 dp·sp로 그대로 사용
- 남는 세로 공간(현재 약 90dp)은 홈의 비행 무대가 가져감

### 에뮬레이터 미지원 (차량 설정이 EV라서)

| 항목 | 이유 |
|------|------|
| 연료량/연비 4종 (Fuel API) | VHAL에 연료 속성이 없음. EV 배터리로 대체 |
| EV 전비 4종 (Instantaneous/Current/SinceCharge/Cumulative) | VHAL에 해당 vendor 속성(0x21600b13~15)이 없음. **앱에서 잔량 감소량 ÷ 거리 증가량으로 직접 계산** |
| 이번 주행 거리/시간 (Trip) | SDK `getTripCurrentDrivingDistance/Time` 미지원. **앱에서 총 주행거리 차이로 계산** |
| 드라이브 모드 (ECO/SPORT) | VHAL에 속성 없음 |
| 점검 (엔진오일, 브레이크액, 워셔액) | VHAL에 속성 없음 |
| 충전 진행 (EV_CHARGE_STATE, 속도, 남은 시간) | 값은 읽히나 콘솔로 바꿔도 **고정값만 반환** |

### 권한 오류 (서드파티 불가)

| 항목 | 필요 권한 |
|------|----------|
| 경고등 (Telltales) | `PRIVATE_CAR_SAFETY` |
| 타이어 공기압 | `PRIVATE_CAR_SAFETY` |
| AAOS 주행거리 (PERF_ODOMETER) | `CAR_MILEAGE` (시스템 전용) → Vehicle SDK `getCurrentDistanceDriven` 사용 |
| AAOS 좌석 탑승/벨트 | `CONTROL_CAR_SEATS` (시스템 전용) → Vehicle SDK `Seat` 사용 (값이 반대로 옴, 실차 확인 필요) |

### 주의 사항

- **조향각 부호가 반대**: 콘솔 steering=+90이 SDK에서는 -90으로 옴. 앱에서 부호를 뒤집어야 함
- **좌석 탑승 값이 반대**: 콘솔 Occupied=true가 SDK에서는 false로 옴. 실차에서 확인 필요
- **TTS·STT**: `initialize`는 성공하나 `registerApp`에 Client ID/Secret 필요 (local.properties에 설정)

### 주행 중 화면 표시 (probe-results/RESULTS.md)

주행 상태 (gear=Drive, speed=30): UxRestrictions DO=true, UxR=0x50 (NO_KEYBOARD | NO_VIDEO)

| 방법 | 결과 | 비고 |
|------|------|------|
| **distractionOptimized 메타데이터** | ✅ 가능 | AndroidManifest의 Activity에 `<meta-data android:name="distractionOptimized" android:value="true" />` 추가. 주행 중에도 앱 전체 화면 표시 가능 (2026.11.08 실측) |
| **NAVIGATION HUN** | ✅ 가능 | 채널 IMPORTANCE_HIGH + setCategory(NAVIGATION). 상단 흰 카드(720x160px), 갱신 시 깜빡임 없음, **타임아웃 없이 무한 유지**. 0.25초 애니메이션 가능. 주행 중에도 동일 표시 |
| **오버레이 창 (TYPE_APPLICATION_OVERLAY)** | ⚠️ 부분 가능 | 상단바 아래·화면 모서리에 상시 캐릭터 가능. 주행 중(D 30km/h)에도 30초 이상 유지 확인. **단, 상단바 위로는 불가**. 권한이 adb로만 허용 가능(설정 화면 없음) |
| 일반 알림·FGS 알림·토스트 | ❌ 불가 | 상단바·종·알림 센터 어디에도 안 보임 |
| 상단바 아이콘 슬롯 등록 | ❌ 불가 | STATUS_BAR 권한 거부 + 숨겨진 API 차단 |

## 확인 필요 (실차에서)

- 백그라운드에서 상시 기록(30초 링버퍼)이 허용되는지
- 조향각 부호: 실차에서 어느 쪽이 양수인지
- 좌석 탑승 SDK 값 의미
- EV 전비 SDK가 실차에서 지원되는지, 단위는 무엇인지
- 과속방지턱 (`onCautionPointInfo`) 콜백이 경로 안내 중에 오는지

## 현재 공개 문서에 없거나 미지원
- 가속 페달 위치, 가속도(G센서), 안전벨트 상태
- ADAS SDK: 문서상 "준비 중", 호환성 표상 미지원
- 빌트인 캠 영상·이벤트 접근 API 없음
- → 공모전 미공개 SDK로 제공되면 '확장 계획'으로 반영

## 플랫폼 정책
- 차량 하드웨어 직접 접근 금지, 플랫폼 API로만 접근
- 성능 권장: CPU 3% 이하, RAM 400MB 이하
- 불필요한 상시 백그라운드 실행 지양
- 주행 중 앱 사용은 원칙적으로 제한(카테고리별 예외)
- 차량 ID·사용자 ID 가명화, 주행 데이터 마케팅 사용 금지

## 개발 환경 (공모전 설명회)
- Android Studio, Java/Kotlin, 최소 Android API 28, Gradle 8.0 이상
- Pleos Connect 에뮬레이터 + 버추얼 콘솔(기어, 도어, 배터리 등 차량 상태를 가상으로 바꿔 앱 반응 검증)
- 시작: Getting Started 문서 → 프로젝트 생성 → 에뮬레이터 → Vehicle SDK 연동 → 첫 API 호출(예: 도어 상태)
- 공모전·개발 단계: 권한 신청·승인 불필요. AndroidManifest에 문서가 안내하는 uses-permission만 선언하면 에뮬레이터에서 호출 가능
- 실제 Pleos App Market 배포 시: 심사에서 승인된 권한만 선언해야 하며, 미승인 권한 선언 시 심사 탈락

## 참고 문서
- SDK 개요: https://pleos.ai/playground/resources/api-reference/connect-sdk
- Vehicle SDK: https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/intro
- 호환성 표: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability
- NaviHelper: https://pleos.ai/playground/resources/api-reference/connect-sdk-pleos/NaviHelper/intro
- 성능 기준: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/app-runtime-environment-resources-guide/app-performance-platform-identify
- 보안·개인정보: https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/security-privacy
