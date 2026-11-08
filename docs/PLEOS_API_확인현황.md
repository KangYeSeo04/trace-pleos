# Pleos API 확인 현황 (2026.10.29 기준, 공개 문서 기준)

## 확인된 것 (핵심 기능에 사용)
- 차량 속도: AAOS PERF_VEHICLE_SPEED (CarPropertyManager, Car.PERMISSION_SPEED)
- 브레이크: Vehicle SDK Brake API (브레이크 페달·주차 브레이크 상태 조회)
- 조향각: Vehicle SDK Steeringwheel API (getSteeringWheelAngle, 변경 콜백)
- 방향지시등: Vehicle SDK TurnSignal API (상태 조회)
- 기어: AAOS GEAR_SELECTION 사용 가능 (CURRENT_GEAR는 불가)
- 위치: NaviHelper getCurrentLocationInfo
- 음성: Gleo AI STT·TTS (전 차종 호환)
- 서드파티 앱은 AAOS 차량 속성을 읽기만 가능(쓰기 불가)

## 확인 필요
- 브레이크 값이 밟음 여부인지, 밟은 정도(%)인지
- 속도·조향각의 실제 갱신 주기
- 백그라운드에서 상시 기록(30초 링버퍼)이 허용되는지
- 버추얼 콘솔에서 조작 가능한 상태값 목록

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
