# 라이트 모드 디자인 (2026-11-09 추가)

출처: 디자인 캔버스 "TRACE 정지 홈 · 우주 비행" 새 버전(라이트 · 파란 하늘 · 비행기).
상점·내 우주, "비행 · 구매 상품(새·열기구·복엽기)"은 이번 범위가 아니라 넣지 않았다.

## html/ (라이트)

| 파일 | 화면 | 원래 캔버스 이름 |
| --- | --- | --- |
| HomeLight.dc.html | 홈 | Main.dc.html |
| SpaceLight.dc.html | 비행 · 구름 (홈 하늘 탭) | SpaceLightCloud.dc.html |
| EffLight.dc.html | 전비 상세 | 같음 |
| MaintLight.dc.html | 정비 상세 | 같음 |
| CoinLight.dc.html | 코인 상세 | 같음 |
| RegionLight.dc.html | 지역 상세 | 같음 |

HTML 안의 링크(`Main.dc.html` 등)는 캔버스 이름 그대로다.

## CoinDark.dc.html 갱신

새 캔버스에서 다크 코인 화면도 바뀌었다. 기존 `design/html/CoinDark.dc.html`을 이 파일로 덮어쓴다.
바뀐 곳: "이번 주행 1km 기록"이 별 격자에서 **가로 막대(적립·멈춤 구간)** + 범례(적립 금색, 멈춤 회색)로 바뀜.
상단 히어로는 다크만 canvas 애니메이션, 라이트는 정지 사진(`sky_day_hero.jpg`).

## 라이트 토큰 (HTML의 T 객체 그대로)

| 이름 | 라이트 | 다크(참고) |
| --- | --- | --- |
| panel | #FFFFFF | #17191D |
| tile | #F3F4F6 | #202328 |
| line | #E5E7EB | #2C3036 |
| t1 | #111418 | #F2F3F5 |
| t2 | #5C636D | #A3A9B2 |
| t3 | #8A9099 | #7C828B |
| accent | #1677E6 | #4C9AFF |
| good | #12935F | #38C08A |
| goodSoft | #E3F3EB | rgba(56,192,138,0.14) |
| caution | #D9820B | #F0A33C |
| bar | #DADDE2 | #363A41 |
| sel | #FFFFFF | #17191D |

warn, gold, 지도 색(바다·땅·선·라벨), 지역 도별 색 등 위 표에 없는 값은 각 Light HTML에서 찾는다.

## assets/ (라이트) — HTML 속 `/_blob/…` 대응

| HTML 속 주소 | 파일 | 크기 | 쓰이는 곳 |
| --- | --- | --- | --- |
| `/_blob/221145b4cfc19063eb91149f8c021461` | sky_day_hero.jpg | 1120 × 500 | 홈 하늘(라이트), 코인 상단(라이트) |
| `/_blob/182233b6705c5f4ffdaf8ca78b96bc80` | cloud_flight.mp4 | 1.0 MB | 비행 · 구름 화면 반복 영상 |

다크와 같이 쓰는 것: `0cd07762…`(shuttle.png). `40dc1382…`, `d30d11dd…`(곰 자동차)는 꺼진 예비 옵션이라 쓰지 않는다.

## screenshots/ (라이트)

HomeLight, SpaceLight(영상 한 장면), EffLight, MaintLight, CoinLight, RegionLight, 그리고 갱신된 CoinDark.
2배율(가로 1200 = 디자인 600). 라이트는 바탕이 흰색이라 아래 여백을 자르지 않았다.
