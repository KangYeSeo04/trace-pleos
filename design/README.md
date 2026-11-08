# design/ — 디자인 원본

디자인 캔버스(TRACE 정지 홈 · 우주 비행)의 **다크 모드** 화면 원본입니다.

## html/

각 파일은 화면 하나의 HTML 원본입니다. 모양은 inline style, 값과 분기는 파일 아래쪽 `<script type="text/x-dc">`의 `renderVals()`와 `TOK` 객체에 있습니다.

| 파일 | 화면 | 디자인 크기(px) |
| --- | --- | --- |
| HomeDark.dc.html | 삼분할 홈 | 600 × 800 |
| SpaceDark.dc.html | 우주 비행 | 600 × 800 |
| EffDark.dc.html | 상세 · 전비 | 600 × 2000 (세로 스크롤) |
| MaintDark.dc.html | 상세 · 정비 (+ 기록 추가) | 600 × 2600 |
| CoinDark.dc.html | 상세 · 코인 | 600 × 1900 |
| ShopDark.dc.html | 상점 | 600 × 1000 |
| RegionDark.dc.html | 상세 · 지역 | 600 × 1700 |
| UniverseDark.dc.html | 상점 · 내 우주 | 600 × 800 |

HTML 안의 링크(`href="HomeDark.dc.html"` 등)는 화면 이동을 뜻합니다.

## assets/ — HTML의 `/_blob/…` 주소와 파일 대응

HTML에는 이미지가 `/_blob/<id>` 주소로 들어 있습니다. 아래 표로 실제 파일을 찾으세요.

| HTML 속 주소 | 파일 | 크기 | 쓰이는 곳 |
| --- | --- | --- | --- |
| `/_blob/8afe44cb1d8315edc9ce95b2557ce94a` | sky_night_hero.jpg | 1120 × 500 | 홈 무대(heroImg, 다크) |
| `/_blob/7690a00a158ea2b8995cd1067fb42321` | sky_night_hero.jpg (같은 파일) | 1120 × 500 | 상점 미리보기 · 기본 비행운 |
| `/_blob/b9e431f53bf9928928cb93e6481f7486` | sky_night_hero_stardust.jpg | 1120 × 500 | 상점 미리보기 · 별가루 비행운 |
| `/_blob/98a0cbf990c7c7eb4e46483f72ffe6d6` | coin_bg_night.jpg | 736 × 1308 | 코인 상단 배경(다크) |
| `/_blob/0cd07762b33ca0752ad5355276746d0a` | shuttle.png | 460 × 425 | 코인 캔버스 기체, 내 우주 우주왕복선 |
| `/_blob/24669182514edcbcccbbf9c9982ee3dd` | space_flight.mp4 | 2.7 MB | 우주 비행 화면 반복 영상 |

### 넣지 않은 것

| HTML 속 주소 | 이유 |
| --- | --- |
| `/_blob/eb1cd6dd…`, `81a962f3…`, `d5a8c2c2…`, `93ddc586…` | Pretendard 400·500·600·700 웹 폰트(woff). Android에서 못 쓰므로 공식 Pretendard OTF를 받아 `res/font/`에 넣는다 |
| `/_blob/40dc1382…`, `d30d11dd…` | "곰이 운전하는 자동차". 꺼져 있는 예비 옵션이라 쓰지 않는다 |
| `/_blob/8eb9d004…`, `11428b7d…`, `5971375b…`, `14b8d275…` | 라이트 모드 이미지. 라이트 단계에서 추가 |

## screenshots/

각 HTML을 렌더링한 캡처입니다. 파일 이름은 HTML과 같습니다.

- **2배율**로 찍었습니다. 가로 1200px = 디자인 600px. 에뮬레이터 캡처와 비교할 때 절반으로 줄여 보세요.
- 아래쪽 빈 공간은 잘라 냈습니다. 그래서 세로 길이가 캔버스 보드 높이보다 짧습니다.
- 홈 무대와 코인 상단은 움직이는 그림이라, 캡처는 그 한 순간입니다. 움직임은 HTML의 스크립트가 기준입니다.
- 우주 비행(`SpaceDark.png`)은 영상의 한 장면입니다.

## fonts/

Pretendard 1.309 (400·500·600·700), 디자인에 쓰인 웹 폰트를 Android용 OTF로 바꾼 것입니다. 글자 수 11,892개 전체가 들어 있습니다. 0단계에서 `app/src/main/res/font/`로 옮깁니다.

라이선스: SIL Open Font License 1.1 (앱에 넣어 배포 가능). 앱의 오픈소스 라이선스 고지에 "Pretendard, Copyright (c) 2021 Kil Hyung-jin, SIL OFL 1.1"을 적습니다.
