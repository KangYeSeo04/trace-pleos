# 전체 화면(이분할) 디자인 (2026-11-12 추가)

출처: 디자인 캔버스 "TRACE 정지 홈 · 우주 비행"의 "전체 화면" 페이지.
기준 크기 **1200 × 800** (왼쪽 주행 정보 화면을 뺀 TRACE 칸). 3분할은 600 × 800.
기능·데이터는 3분할과 같고, **배치와 사진·영상만** 다르다.

이번 범위가 아닌 것: 비행·구매 상품(새·열기구·복엽기), 우주 비행·구매 상품, 상점, 상점·내 하늘, 상점·내 우주.

## html/

| 파일 | 화면 | 디자인 높이 | 배치 |
| --- | --- | --- | --- |
| FullLight / FullDark | 홈 | 800 | 넓은 하늘 사진 + 타일 4개 한 줄 |
| FullSpaceLightCloud / FullSpaceDark | 하늘 탭 영상 | 800 | 영상 + 뒤로 버튼 |
| FullEffLight / FullEffDark | 전비 | 1080 | 위: 90일 평균·요약 4칸 / 아래 2열: 왼쪽 충전 타이밍·달력, 오른쪽 월별 전비·이번 달 예측 |
| FullMaintLight / FullMaintDark | 정비 | 1340 | 위: 방문 감지 카드 / 2열: 왼쪽 점검 항목, 오른쪽 정비 달력·기록 추가 |
| FullCoinLight / FullCoinDark | 코인 | 1060 | 위: 넓은 사진 히어로 / 2열: 왼쪽 안전점수·1km 기록, 오른쪽 상점 카드·받은 기록 |
| FullRegionLight / FullRegionDark | 지역 | 800 | 왼쪽 지도, 오른쪽 선택 지역 카드·곧 만나요·위치 기록 |
| Splash | 스플래시 | 800 | (선택) 비행기가 날아와 비행운을 그리고 TRACE 제목 |

주의: 전체 화면 코인 디자인에는 "차량 경고"와 "끼어들기… 빼고 셌어요" 문구가 있지만,
앱은 3분할과 같이 **급정거**를 쓰고 그 문구는 넣지 않는다(ADAS 미지원).
코인 히어로는 전체 화면에서 다크도 **정지 사진**이다(3분할 다크는 canvas 애니메이션).

## assets/ — HTML 속 `/_blob/…` 대응

| HTML 속 주소 | 파일 | 크기 | 쓰이는 곳 |
| --- | --- | --- | --- |
| `/_blob/f62cbac6682d14a592172f44ac2c7130` | sky_night_hero_wide.jpg | 2320 × 880 | 전체 화면 홈·코인 히어로(다크) |
| `/_blob/8177ffa4334c1d9c75e69f52b195257f` | sky_day_hero_wide.jpg | 2320 × 880 | 전체 화면 홈·코인 히어로(라이트) |
| `/_blob/91e76999c707a53da567675adfe14dba` | space_flight_wide.mp4 | 1920 × 1280, 24초, 4.4MB | 전체 화면 하늘 탭(다크) |
| `/_blob/7a7bfd4df758137affd5dcabfb4f490e` | cloud_flight_wide.mp4 | 1920 × 1280, 24초, 1.2MB | 전체 화면 하늘 탭(라이트) |
| `/_blob/5c189b3eae28d3ce405790216a74a2db` | splash_trail.png | 2400 × 1600 | 스플래시 비행운 |
| `/_blob/d4aef1a423f7edd290e8f436b6b22efe` | splash_plane.png | 239 × 222 | 스플래시 비행기 |

`40dc1382…`, `d30d11dd…`(곰 자동차)는 꺼진 예비 옵션이라 쓰지 않는다.

## screenshots/

각 Full*.png와 Splash.png. 2배율(가로 2400 = 디자인 1200).
