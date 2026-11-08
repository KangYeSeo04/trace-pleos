# TRACE — Claude Code 작업 지침

이 파일은 저장소 루트에 둔다. Claude Code는 매 작업 전에 이 파일을 기준으로 삼는다.

## 1. 프로젝트 한 줄

TRACE는 플레오스 커넥트(AAOS 기반 차량 OS)용 Android 앱이다. 흩어진 차 정보를 밤하늘의 비행기와 비행운으로 보여 주고, 카드 4장(전비·정비·코인·지역)으로 요약한다. 현대차·기아 차량용 앱 공모전 출품작이며, 12월 앱 시연 평가가 목표다.

- 서류 접수: 11/15 · 시연 리허설: 12/2
- 이번 단계 범위: **다크 모드만**. 라이트 모드는 나중에 토큰만 추가해 붙인다.

## 2. 기술 스택 (바꾸지 말 것)

- Kotlin, Jetpack Compose 단일 앱(Activity 1개), Material3는 쓰되 기본 테마 색은 쓰지 않는다
- minSdk 28, Gradle 8.0 이상, Android Studio 최신 안정판
- 구조: `ui`(화면) / `domain`(계산) / `data`(SDK·저장) 3계층, 화면은 `UiState`만 받아 그린다
- 상태: ViewModel + StateFlow
- 저장: Room, 주기 작업: WorkManager
- 영상: Media3 ExoPlayer (우주 비행 화면만)
- 차량 연동: Pleos Connect Vehicle SDK, AAOS CarPropertyManager, NaviHelper, Gleo AI TTS·STT

새 라이브러리를 추가해야 하면 먼저 이유를 말하고 확인받는다.

## 3. 참고 자료 위치

| 경로 | 내용 |
| --- | --- |
| `docs/기획안.md` | 기능정의서(A-01~H-01), 화면설계서(S01~S08), 공통 규칙. **기능과 동작의 기준** |
| `docs/PLEOS_API_확인현황.md` | 확인된 API / 확인 필요 / 미지원 목록. **SDK 사용의 기준** |
| `design/html/*Dark.dc.html` | 디자인 원본(HTML). **모양의 기준**. 색·크기·간격·애니메이션 수식을 여기서 그대로 읽어 옮긴다 |
| `design/assets/` | 디자인에 쓰인 이미지·영상 원본. HTML 속 `/_blob/<id>` 주소와 파일의 대응은 `design/README.md`에 있다 |
| `design/screenshots/` | 화면별 캡처(2배율, 가로 1200 = 디자인 600). 에뮬레이터 결과와 나란히 비교할 때 쓴다 |
| `design/fonts/` | Pretendard OTF 4종 |

### 디자인 파일 ↔ 화면 ID

| 디자인 파일 | 화면 | 기획안 ID |
| --- | --- | --- |
| `HomeDark.dc.html` | 삼분할 홈 (정차·주행 두 모드) | S01, S02 |
| `SpaceDark.dc.html` | 우주 비행 (영상 + 뒤로 버튼, 정보 없음) | 신규 |
| `EffDark.dc.html` | 전비 상세 | S03 |
| `MaintDark.dc.html` | 정비 상세 + 기록 추가 | S04, S05 |
| `CoinDark.dc.html` | 코인 상세 (상단 캔버스 애니메이션) | S06 |
| `ShopDark.dc.html` | 상점 · 꾸미기 | S07 |
| `RegionDark.dc.html` | 지역 상세 (시·군·구 경계 path) | S08 |
| `UniverseDark.dc.html` | 내 우주 (행성 드래그, 두 손가락 확대·축소) | 신규 |

### 디자인 파일을 읽을 때 주의

- 파일은 `<x-dc>` 마크업 + `<script type="text/x-dc">` 로직이다. 화면 모양은 마크업의 inline style, 값과 분기는 `renderVals()`와 `TOK` 객체에 있다.
- `<sc-if>`는 조건부 표시다. **기본값이 false인 분기는 실제 디자인이 아니다.** 특히 `HomeDark.dc.html`의 "곰이 운전하는 자동차"(`charLight`, `charDark`, `traceMode`)는 꺼진 예비 옵션이므로 옮기지 않는다. 홈 무대는 `heroImg`(밤하늘 + 비행기 사진)다.
- 디자인에 보이는 숫자(5.8 km/kWh, 1,284 코인 등)는 예시 값이다. 앱 코드에 하드코딩하지 말고 `@Preview`용 샘플 상태로만 쓴다.

## 4. 디자인 이식 규칙

### 단위

- 디자인 기준 프레임: 삼분할 오른쪽 칸 **600 × 800 CSS px**
- 변환: `scale = min(칸가로/600, 칸세로/800)`. 실행 시 칸 크기로 동적 계산.
- **TraceTheme 최상위**에서 `LocalDensity`를 `density × scale`로 덮어써서 제공. 화면 코드는 디자인 px 숫자를 dp·sp로 그대로 쓴다. 시스템 글자 크기 설정(fontScale)은 유지.
- 글자 크기도 같은 비율(px → sp). 숫자는 고정폭(`fontFeatureSettings = "tnum"`).
- 가로 기준으로 키우고 남는 세로 공간은 홈의 비행 무대 높이가 가져간다. 상세 화면은 세로 스크롤이라 아래로 길어진다.
- 실측(2026.11.08): 에뮬레이터 2560×1440 px, 160dpi, 3분할 TRACE 칸 841×1210 dp, scale 1.40.

### 다크 테마 토큰 (`ui/theme/TraceColors.kt`)

| 이름 | 값 | 쓰임 |
| --- | --- | --- |
| panel | `#17191D` | 화면 바탕 |
| tile | `#202328` | 카드·타일 바탕 |
| t1 | `#F2F3F5` | 주요 글자 |
| t2 | `#A3A9B2` | 보조 글자 |
| t3 | `#7C828B` | 흐린 글자, 라벨 |
| track | `#33373E` | 막대 바탕, 구분선 |
| accent | `#4C9AFF` | 강조 파랑 (유일한 강조색) |
| good | `#38C08A` | 좋음 |
| caution | `#F0A33C` | 주의 |
| warn | `#EF7A70` | 경고 |
| gold | `#E9B949` | 코인·별 |

- 위 표에 없는 색이 디자인에 있으면 HTML에서 값을 찾아 토큰으로 추가하고, 화면 코드에 hex를 직접 쓰지 않는다.
- 상태색(good/caution/warn)은 항상 글자와 함께 쓴다.

### 글꼴

- Pretendard 400·500·600·700. `design/fonts/pretendard_*.otf`를 `res/font/`로 옮겨 쓴다(SIL OFL 1.1).
- 숫자 일부에 Inter가 쓰였다. HTML의 `font-family`를 확인해 같은 곳에만 Inter를 쓴다.
- 글자 간격: 기본 `-0.01em`.

### 그리기

| 디자인 기법 | Compose |
| --- | --- |
| flex / gap | `Row`/`Column` + `Arrangement.spacedBy` |
| 선 SVG 아이콘 | `ImageVector` (`ui/icons/TraceIcons.kt`에 모음) |
| `<canvas>` + requestAnimationFrame | `Canvas` + `withFrameNanos`. JS의 수식·상수를 그대로 옮긴다 |
| 지역 경계 SVG path | `PathParser().parsePathString(d).toPath()`, path 데이터는 `assets/`의 JSON으로 분리 |
| box-shadow | `Modifier.shadow` 근사, 눈으로 맞춘다 |
| backdrop-filter blur | API 31+ `RenderEffect`, 28~30은 반투명 배경으로 대체 |
| `<video loop muted>` | Media3 ExoPlayer, 반복·음소거 |
| CSS @keyframes | `rememberInfiniteTransition` |

### 공통 동작 (기획안 공통 규칙에서)

- 상태 전환 1초 크로스페이드, 화면 이동 300ms. 깜빡임·팝업·빠른 반복 애니메이션 없음.
- 터치 영역 최소 44dp. 타일 전체가 버튼.
- 주행이 시작되면 상세·상점·기록 추가 화면에서 홈으로 자동 복귀(A-02). 작성 중 기록은 임시 저장.
- 접근성 "모션 줄이기"가 켜져 있으면 움직임 1/5 속도(디자인의 `prefers-reduced-motion` 처리와 같음).
- 모든 아이콘 버튼에 `contentDescription`.

## 5. 차량 데이터 규칙

- **앱 코드에 가짜 데이터나 시연용 분기를 넣지 않는다.** 실차와 에뮬레이터에서 같은 코드가 돈다.
- 샘플 데이터는 `@Preview`와 테스트 코드에서만 쓴다.
- 차량 신호는 `data/vehicle/VehicleSignals` 인터페이스 하나로 받는다. 구현체는 SDK용 하나. 화면과 계산 로직은 SDK를 직접 부르지 않는다.
- `docs/PLEOS_API_확인현황.md`에서 "확인됨"인 API만 핵심 기능에 쓴다. "확인 필요"는 코드에 `// TODO(확인필요): …`를 남기고, "미지원"(가속 페달, G센서, 안전벨트, ADAS, 빌트인 캠)은 쓰지 않는다.
- 앱 시작 시 `checkXXXCapability`로 지원 여부를 확인하고, 미지원이면 대체 계산으로 전환한다(A-06).
- 서드파티는 차량 속성 **읽기만** 가능. 쓰기 API를 부르지 않는다.
- 성능: CPU 3% 이하, RAM 400MB 이하. 원시 신호는 계산 후 바로 버리고 요약만 저장한다.
- 위치 기록은 기기 밖으로 보내지 않는다. 네트워크 전송 코드를 만들지 않는다.
- 차가 직접 주지 않는 값(전비 등)은 화면에 "추정"을 표시한다.

## 6. 개발 단계

각 단계는 브랜치 하나 이상, 끝나면 main에 합친다. 단계 안의 작업은 위에서부터 한다.

### 0단계 · 기반 (`chore/setup`)
1. 기존 프로젝트(패키지 `com.tracers.trace`)를 정리한다. 새 프로젝트를 만들지 않는다. Gradle 설정·`.gitignore` 점검.
   - `app/src/main/java/com/tracers/trace/probe/`, `pip-probe/`, `topbar-probe/`, `probe-results/`, `tools/`는 기획 단계에서 SDK를 직접 호출해 본 실험 코드와 결과다. 지우지 말고, 살릴 SDK 호출 코드는 `data/vehicle/`로 옮기는 안을 먼저 제안한다.
2. 폴더 구조(`ui/`, `domain/`, `data/`), 단일 Activity + Navigation Compose
3. `TraceColors`, `TraceType`(Pretendard, `design/fonts/`에서 복사), `TraceDimens`(scale), `TraceTheme`
4. `design/assets/`의 이미지·영상을 `res/drawable-nodpi/`, `res/raw/`로 정리(파일명은 소문자_스네이크)
5. 확인: 빈 화면이 다크 패널 색으로 뜨는지 에뮬레이터 실행

### 1단계 · 디자인 (화면당 브랜치 `design/<id>-<이름>`)
순서: 홈(S01·S02) → 우주 비행 → 전비(S03) → 코인(S06) → 정비(S04·S05) → 지역(S08) → 상점(S07) → 내 우주

화면마다:
1. 해당 `*Dark.dc.html`을 끝까지 읽고, 화면에 필요한 `UiState` 데이터 클래스를 먼저 정의
2. 공통 컴포넌트가 생기면 `ui/components/`로 뺀다(타일, 막대, 칩, 헤더, 뒤로 버튼 등)
3. 화면 Composable 작성 + `@Preview(widthDp = 600, heightDp = 800)` 샘플 상태
4. 상세 화면은 디자인 높이 전체(예: 전비 2000)가 세로 스크롤로 보이게
5. 확인: 에뮬레이터 캡처를 `design/screenshots/`의 같은 화면과 나란히 두고 색·간격·글자 크기 차이를 목록으로 보고한 뒤 고친다

### 2단계 · 기능 (`feat/<기획안ID>-<이름>`)
기획안의 P0 먼저, 그다음 P1. 권장 순서:
1. A-01 주행 상태 판정(기어 + 차속, 2초 유지) → A-02 자동 복귀 → B-05 타일 주행/정차 모드
2. A-06 차종 지원 확인
3. C-01·C-02 전비·충전 타이밍 → E-01·E-02·E-03·E-04 안전점수·코인
4. B-03·B-04 기체 상태와 거칠었음 감지
5. G-01·G-04 지역 판정·위치 기록 관리 → D-01 점검 항목
6. A-04 주행 중 음성 안내(TTS)
7. P1: C-03·C-04, D-02~D-05, F-01, G-02·G-03, B-02

기능마다:
1. 계산은 `domain/`의 순수 Kotlin 클래스로, 단위 테스트를 먼저 쓴다(예: 안전점수 = 기존 × 0.98 + 이번 1km × 0.02, 시작 85점)
2. `data/`에서 SDK 연결 → ViewModel이 `UiState`로 변환
3. 확인: 버추얼 콘솔로 기어·속도·배터리를 바꿔 화면 반응을 확인

### 3단계 · 시연 준비 (`feat/demo-replay`, `fix/…`)
1. PC에서 버추얼 콘솔에 시나리오(출퇴근·충전·주말 나들이)를 시간 순서대로 넣는 재생 스크립트(`tools/replay/`). 앱 코드는 건드리지 않는다
2. 화면 비율 최적화(삼분할 600×800, 이분할 1280×800)
3. 성능 측정(CPU·RAM), 안정화

## 7. 깃 규칙

### 브랜치
- `main`: 항상 빌드되고 에뮬레이터에서 실행되는 상태
- 작업 브랜치: `chore/…`, `design/<id>-<이름>`, `feat/<기획안ID>-<이름>`, `fix/<내용>`
- main에 직접 커밋하지 않는다. PR로 합친다.

### 커밋
- **빌드가 되는, 한 문장으로 설명되는 변경 하나 = 커밋 하나**
- 커밋 전에 `./gradlew assembleDebug`와 `./gradlew testDebugUnitTest`가 통과해야 한다
- 메시지: `<종류>(<ID>): <무엇을>` 한국어

| 종류 | 언제 |
| --- | --- |
| `design` | 화면 모양, 색, 폰트, 자산 |
| `feat` | 기능, SDK 연결 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 같고 구조만 정리 |
| `test` | 테스트만 추가·수정 |
| `chore` | Gradle, 설정, 문서 |

예: `design(S01): 홈 정차 모드 타일 4종 배치`, `feat(A-01): 기어·차속으로 주행 상태 판정`

### 푸시와 PR
- 작업 브랜치는 커밋할 때마다 푸시, 늦어도 하루 작업 끝에는 반드시 푸시
- 화면 하나 또는 기능 하나가 끝나고 에뮬레이터 확인까지 마치면 PR을 연다
- PR 본문: 무엇을 했나 / 기획안 ID / 확인한 방법 / 디자인과 남은 차이 / 스크린샷
- **푸시와 PR 생성, main 병합은 사용자에게 확인받은 뒤에 한다**

### 태그
| 태그 | 시점 |
| --- | --- |
| `v0.1-docs` | 11/15 서류 접수 (신호 수집, 전비, 홈, 시연 영상용) |
| `v0.2` | 11/23 (안전점수·코인, 충전 기록) |
| `v0.3` | 11/30 (정비, 지역, 음성) |
| `v1.0-demo` | 12/2 시연 최종본 |

### 올리지 않는 것
`local.properties`, `build/`, `.gradle/`, `*.jks`, `*.keystore`, API 키, 실차에서 기록한 위치·주행 로그. 5MB가 넘는 자산은 Git LFS로 올린다.

## 8. 작업 방식

- 작업을 시작할 때 이번에 할 일을 3~5줄로 말하고 시작한다.
- 한 번에 화면 하나 또는 기능 하나만 한다. 범위를 넘는 변경이 필요하면 먼저 묻는다.
- 디자인과 기획안이 다르면 **디자인을 따르고**, 차이를 보고한다. 기획안과 API 현황이 다르면 **API 현황을 따른다**.
- 모르는 SDK 시그니처는 추측해서 쓰지 않는다. 공식 문서를 확인하거나 사용자에게 묻는다.
- 작업이 끝나면: 바뀐 파일, 확인한 방법, 남은 문제, 다음 할 일을 짧게 보고한다.
