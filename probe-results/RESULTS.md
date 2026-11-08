# Pleos Connect 상단 표시 가능성 테스트 결과

- 환경: Pleos Connect Emulator v2.1.0 (API 34), Cold Boot, 화면 2560x1440 / 160dpi, 현재 사용자 **user 10**
- 테스트 앱: `topbar-probe` 모듈 (`com.probe.topbar`, targetSdk 34, FGS type `specialUse`)
- 주행 상태: 버추얼 콘솔 `gear=Drive, speed=30` → `CarUxRestrictions DO: true, UxR: 0x50` (NO_KEYBOARD | NO_VIDEO). **문자열 길이 제한(0x4)은 이 설정에 없음**
- 상단바 = `TopCarSystemBar` 창, statusBars inset `[0,0][2560,90]` (높이 90px). 왼쪽 0~667px은 drivingview(계기 정보), 오른쪽에 SP0(잠금·검색·종) … SP1(프로필·BT·네트워크)·시계
- 재현 스크립트: `probe.sh` (adb broadcast/서비스 실행, 기어 전환, 증거 저장)

## 결과 표

| 테스트 | 정차(P) 결과 | 주행(D) 결과 | 스크린샷 | 결론 |
|---|---|---|---|---|
| T1 일반 알림·종 표시 | 아무 표시 없음. 종 아이콘 안 생김, 알림 센터에도 안 들어감 (NotificationCenter 로그 없음) | 동일 | T1_P_before/after, T1_T2_statusbar_compare | ❌ DEFAULT 알림은 Pleos UI 어디에도 안 보임 |
| T2 HIGH 무카테고리 HUN | 헤드업 안 뜸, 종 없음 | 동일 | T2_P_posted, T2_D_posted | ❌ AAOS 규칙 그대로 (카테고리 필수) |
| T3 NAVIGATION HUN 유지 | ✅ 즉시 뜸. 5초 갱신 60초간 계속 유지, 갱신 시 깜빡임·재등장 없음 (`already shown … just update it, timeoutAfter: 0`). **갱신 멈춘 뒤에도 사라지지 않음** (60초 넘게 유지 확인). 가로 스와이프로 닫힘 → 알림 센터 "일반 알림"에 **남음**. 닫은 뒤 다시 갱신하면 다시 뜸 | ✅ 동일 (60초 유지, 문자열 잘림 없음, 갱신 중단 후 40초 넘게 유지) | T3_P_0s/30s/60s/stop_plus10s, T3_P_still_shown_63s_after_stop, T3_P_after_swipe, T3_P_notif_center_after_dismiss, T3_P_reupdate_after_swipe, T3_D_*, HUN_park_vs_drive_compare | ✅ **상단 상시 표시 가능**. 카드 위치 x≈920~1640, y≈40~200px (약 720x160), 상단바 SP0 영역(검색·종)을 덮고 앱 영역까지 내려옴 |
| T4 큰 아이콘 애니메이션 | 1초/0.5초/0.25초 모두 프레임이 매번 바뀜. 0.25초에서 30초간 119번 게시 → 119번 렌더. rate limit 경고 없음 | 동일 (렌더 수 = 게시 수) | T4_P_*ms_frames(화면녹화 8fps 타일), T4_P_*ms.mp4, T4_D_* | ✅ 0.25초 주기 애니메이션 가능. 큰 아이콘은 76x76으로 축소(`android.reduced.images`) |
| T5 MESSAGE HUN | 뜸. **정확히 10초 후** 자동으로 사라짐. 재생·음소거 같은 추가 버튼 없음 | 동일 (10초) | T5_P_posted, T5_P_plus12s, T5_D_posted | 참고: NAVIGATION과 달리 시간 제한 있음 |
| T6 포그라운드 서비스 알림 | LOW·DEFAULT 둘 다 상단바·종·알림 센터 어디에도 안 보임 | 동일 | T6_P_LOW/DEFAULT, T6_D_*, T6_statusbar_compare_* | ❌ FGS 알림은 보이지 않음. 백그라운드 실행 유지 용도로만 쓸 수 있음 |
| T7 백그라운드 토스트 | 안 보임. ToastUI가 받기는 함(`Attempt to hide non-current toast`), 화면에는 안 그려짐 (6초 화면녹화로 확인) | 안 보임 | T7_P_toast_frames, T7_P_toast.mp4, T7_D_toast | ❌ |
| T8 오버레이 | 권한 없음: `canDrawOverlays=false`, `BadTokenException … permission denied for window type 2038`. 오버레이 권한 설정 화면 **없음** (`resolve-activity` → No activity found). appops 강제 허용 후 띠(60px) 오버레이와 64px 캐릭터 오버레이(OverlayService) 모두 뜸. **캐릭터: 내비 화면 위에 뜨고 0.5초 2프레임 애니메이션 정상.** 위치(WM 실제 창 좌표): A `[0,90]` 상단바 바로 아래, B `[2496,90]`, C `[0,1236]`, D `[2496,1236]` (하단바 위). E(LAYOUT_NO_LIMITS) `[0,0]` → 왼쪽 계기 영역 위 y=0에 그려짐. 추가 F(E 플래그+오른쪽 위) `[2496,0]` → **창은 있으나 상단바 아래에 깔려 안 보임**. 홈·내비·앱 종료(태스크 제거, 서비스 유지) 후에도 남음. **단, Car Settings가 앞에 있으면 시스템이 숨김** (`mForceHideNonSystemOverlayWindow=true`, `isVisible=false`), 내비로 돌아오면 다시 보임 | 유지됨 (내비 화면, D 30km/h, 앱 종료 상태에서 30초 넘게 보임). HUN이 오버레이보다 위에 뜸 | T8_01_start, T8_02_pos_A~F, T8_corner_compare_A_E_B_F, T8_corner_compare_C_D, T8_03_home, T8_04_navi, T8_05_settings, T8_05b~09b, T8_character_anim_frames, T8_frames.txt, T8_overlay_log.txt (이전: T8A_P_nav, T8B_*) | ⚠️ 상단바 **아래**·화면 모서리에는 상시 캐릭터 가능, 상단바 **위로는 불가**. 창 순서: TopCarSystemBar > SystemUIOverlay > HeadsUpNotification > … > 앱 오버레이. 일반 사용자가 권한을 줄 화면이 없어 실차 배포는 권한 문제 해결이 전제 |
| T9 edge-to-edge | 효과 없음. 앱은 멀티윈도 패널(1682x1210)과 캡션 바 아래 y=186부터 그려짐. insets 전부 0 | 비DO 앱이라 "주행 중에는 사용할 수 없습니다" 화면으로 가려짐 | T9_P_edge, T9_D_edge, T9_P_log.txt | ❌ 앱 창이 상단바에 닿지 않음 |
| T10 추가 디스플레이 | 추가 디스플레이 없음 (id=0 "기본으로 제공되는 화면" 2560x1440 하나뿐). 다른 디스플레이 실행 시도 대상 없음 | – | T10_P_displays, T10_log.txt, T10_dumpsys_display.txt | 확인 불가 (에뮬레이터에 슬림 디스플레이·클러스터 없음) |
| T11 상태바 슬롯 | `dumpsys statusbar` `mIcons=` 비어 있음. SystemUI 슬롯 36개는 AOSP 기본(bluetooth, location, mobile, mute …)뿐이고 하이패스·다운로드 이름 없음. 상단 아이콘은 `com.android.systemui.car.statusicon.ui.icons.CarStatusItem` 17개로 따로 구현됨. CarSystemUI.apk는 권한 문제로 꺼내지 못함 | – | T11_*.txt | 서드파티가 등록할 슬롯 API 흔적 없음 (StatusBarManager 경로 아님) |
| T12 상단바 아이콘 직접 등록 (RunCat 방식) | ❌ 두 겹으로 막힘. ① `STATUS_BAR` 권한 선언했지만 `granted=false` (시스템 서명 전용). ② `StatusBarManager.setIcon`, `IStatusBarService.setIcon` 둘 다 숨겨진 API 정책으로 리플렉션 차단: `Accessing hidden method … (max-target-r / max-target-o, reflection, denied)` → `NoSuchMethodException`. 호출 후 `mIcons=` 그대로 비어 있음. 우회(hidden API 정책 해제 등)는 지시서 규칙에 따라 하지 않음 | 동일 | T12_P, T12_D, T12_P_log.txt, T12_D_log.txt, T12_permission.txt | ❌ 서드파티 앱은 상단바 안에 아이콘을 넣을 수 없음. 정책을 우회해도 AOSP `StatusBarManagerService.setIcon`이 `STATUS_BAR` 권한을 검사함 (AOSP 소스 기준, 이번에 실측하지는 않음) |

## 상단에 상시 표시가 가능한 방법

**가능: NAVIGATION 카테고리 헤드업 알림 (T3 + T4)**

- 조건: 채널 `IMPORTANCE_HIGH` + `setCategory(Notification.CATEGORY_NAVIGATION)`, 같은 알림 ID로 갱신
- 모양: Pleos 시스템이 그리는 흰 카드 (약 720x160px, 화면 상단 중앙 왼쪽, 상단바 SP0 영역 위로 겹침). 큰 아이콘 원형 76x76 + 제목 1줄 + 본문 1줄. 레이아웃은 바꿀 수 없음 (RemoteViews 미지원)
- 동작:
  - 갱신하면 깜빡임 없이 내용만 바뀜
  - Pleos에서는 **8초 타임아웃이 없음**. 갱신을 멈춰도 계속 떠 있으므로, 안내가 끝나면 앱이 직접 `cancel()` 해야 함
  - 큰 아이콘을 0.25초마다 바꿔도 프레임이 빠지지 않음 → 화살표 깜빡임·회전 같은 간단한 애니메이션 가능
  - 주행 중(D, 30km/h)에도 똑같이 표시됨. 잘림·숨김 없음
- 제약:
  - 사용자가 가로로 밀어 닫을 수 있음. 닫아도 다음 갱신 때 다시 뜸
  - AAOS 규칙과 달리 **알림 센터 "일반 알림"에 남고**, 상단바에 종 아이콘 + 빨간 점이 생김. 안내가 끝나면 cancel로 정리해야 함
  - 카드가 상단바 왼쪽(검색·종 아이콘)을 가림
  - 테스트 중 원인을 알 수 없는 숨김(HIDE)이 몇 번 있었는데, 모두 스크립트가 보내지 않은 터치 입력과 시간이 겹침. 스크린샷·dumpsys로는 숨겨지지 않음 (재테스트로 확인)

**불가**: 일반·HIGH 무카테고리 알림(T1/T2), FGS 알림(T6), 백그라운드 토스트(T7), 상단바 위 오버레이(T8), edge-to-edge(T9), 상단바 아이콘 슬롯 직접 등록(T12)

**부분 가능(권한이 전제)**: 오버레이 창 (T8). 상단바 바로 아래나 화면 모서리에 움직이는 캐릭터를 계속 띄울 수 있지만, 권한 설정 화면이 없어 지금은 adb로만 허용할 수 있음

## T8 캐릭터 오버레이 상세 (OverlayService)

- 구성: 포그라운드 서비스(`specialUse`) + `TYPE_APPLICATION_OVERLAY` 64dp(=64px) 창, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL`, 주황 원 + 눈 2개, 0.5초마다 2프레임 교대 (`T8_character_anim_frames.png`: 4fps 샘플에서 2장씩 번갈아 바뀜)
- 로그: 매번 `canDrawOverlays=true`, addView 예외 없음. 상단 시스템 바 높이 `status_bar_height` = **90px** (dumpsys의 statusBars inset `[0,0][2560,90]`과 일치)
- 좌표 기록 주의: 앱 안의 `getLocationOnScreen` 값은 갱신 직후 틀릴 때가 있어(A가 x=853으로 찍힘), 표에는 `dumpsys window`의 실제 창 frame을 적음 (`T8_frames.txt`)

**① 내비 화면 위에 캐릭터 창이 실제로 뜨는가** → **뜬다.** 내비가 앞에 있을 때 오른쪽 위 `[2496,90]`에 보이고 애니메이션도 돎 (T8_04_navi, T8_05b_navi_after_settings).

**② 상단바 바로 아래나 모서리에 놓을 수 있는가 / E로 상단바를 덮을 수 있는가**
- 상단바 바로 아래(A·B: y=90)와 아래 두 모서리(C·D: 하단바 바로 위 y=1236) 모두 **가능**.
- E(왼쪽 위, NO_LIMITS)는 y=0에 그려지지만, 그 자리는 상단바 아이콘이 아니라 왼쪽 계기(drivingview) 영역 위임 → 주행 정보(km 표시)를 가림.
- 상단바 아이콘 위를 직접 확인하려고 추가한 F(같은 플래그, 오른쪽 위)는 창 좌표가 y=0인데도 **화면에 안 보임** → 상단바가 오버레이보다 위에 그려짐. **상단바는 덮을 수 없음.**

**③ 다른 앱 전환, 테스트 앱 종료, 주행 상태에서도 남는가**
- 홈 버튼, 내비 전환: 남음.
- 테스트 앱 종료(태스크 제거, MainActivity 0개, 서비스는 포그라운드 유지): 남음.
- 주행(D, 30km/h, DO=true): 남음. 앱 종료 + 주행 상태로 30초 넘게 유지 확인 (T8_09b).
- **예외: Car Settings 화면이 앞에 있으면 시스템이 서드파티 오버레이를 숨김** (`mForceHideNonSystemOverlayWindow=true`). 창은 살아 있고 설정에서 나오면 다시 보임. 다른 시스템 앱도 같은 동작을 할 수 있으니 실차 확인 필요.
- 처음 측정한 T8_06_drive_D30, T8_07/08_app_closed는 설정 화면이 앞에 있어 캐릭터가 숨겨진 상태라 무효이고, 내비 화면에서 다시 찍은 T8_06b~09b가 유효 증거임.

## 에뮬레이터 한계로 확인 못 한 것 (실차 확인 필요)

- 실차의 HUN 정책이 에뮬레이터와 같은지 (NAVIGATION 무한 유지, MESSAGE 10초, 알림 센터 보존)
- 실차에서 NAVIGATION HUN을 서드파티 앱에 허용하는지 (내비 앱 전용 정책 여부)
- 실차 주행 중 UX 제한 설정 (에뮬레이터는 0x50만 걸리고 문자열 길이 제한은 없음)
- 슬림 디스플레이·클러스터 존재 여부와 서드파티 실행 가능 여부 (T10)
- 하이패스·다운로드 등 SP1 Dynamic 아이콘 구현 (CarSystemUI.apk 접근 불가)
- 기본 내비가 실제로 안내 중일 때 그쪽 HUN과 우리 HUN이 어떻게 겹치는지 (에뮬레이터에서는 경로 안내를 띄우지 않음)
- 실차에서 오버레이 권한(SYSTEM_ALERT_WINDOW)을 줄 방법이 있는지

## 주최 측에 질의할 것

1. 공모전 앱이 `CATEGORY_NAVIGATION` 헤드업 알림을 주행 중 계속 갱신해 띄우는 것이 허용되는가? 실차에서도 타임아웃 없이 유지되는가?
2. 상단바(SP0/SP1 Dynamic)에 서드파티 아이콘·상태를 등록하는 API나 Pleos SDK 기능이 있는가?
3. 슬림 디스플레이·클러스터에 서드파티 앱이 그릴 수 있는가? 있다면 어떤 API인가?
4. 심사용 실차·에뮬레이터에서 `SYSTEM_ALERT_WINDOW`(오버레이)를 허용받을 방법이 있는가?
5. 주행 중 사용 가능한 앱(distractionOptimized) 지정 절차는? (비DO 앱은 D에서 "주행 중에는 사용할 수 없습니다"로 가려짐 — T9_D_edge)
6. 기본 내비의 경로 안내 HUN과 서드파티 NAVIGATION HUN이 동시에 있을 때 우선순위 정책은?

## 기타 관찰

- 테스트 중 앱 프로세스가 한 번 재시작됨 (pid 8232 → 15974, 주행 전환 이후). 원인은 따로 확인하지 않음
- 화면 캡처(`screencap`)는 연속 호출 시 같은 프레임을 반환할 때가 있어서, T4·T7 판정은 `screenrecord` 영상 기준으로 함
- `T5_D_posted_INVALID_drawer_open.png`: 외부 입력으로 앱 서랍이 열린 상태의 무효 샷. 재측정본은 `T5_D_posted.png`
