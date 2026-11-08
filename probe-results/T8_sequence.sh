#!/bin/bash
# T8 캐릭터 오버레이 시퀀스. 단계마다 스크린샷 + WM 실제 창 좌표 기록
S=/private/tmp/claude-501/-Users-gang-yeseo-AndroidStudioProjects-trace/cb531cc4-38f4-456a-80e2-182898a656c1/scratchpad
cd /Users/gang-yeseo/AndroidStudioProjects/trace/probe-results
F=T8_frames.txt; : > $F
ov(){ adb shell am start-foreground-service --user 10 -n com.probe.topbar/.OverlayService -a $1 >/dev/null; }
step(){ sleep ${2:-2}
  adb exec-out screencap -p > T8_$1.png
  fr=$(adb shell dumpsys window windows | tr -d "\r" | grep -A40 "u10 ProbeCharacter}" | grep -oE " frame=\[[0-9,-]+\]\[[0-9,-]+\]" | head -1)
  top=$(adb shell dumpsys activity activities | grep -m1 topResumedActivity | grep -oE "u10 [^ ]+")
  svc=$(adb shell dumpsys activity services com.probe.topbar | grep -c "OverlayService")
  echo "$1 | window ${fr:-NOT FOUND} | top=$top | overlayServiceRecords=$svc" | tee -a $F; }
adb logcat -c
$S/p.sh park; sleep 2
adb shell am start --user 10 -n com.probe.topbar/.MainActivity >/dev/null; sleep 3
ov START; step 01_start
for p in A B C D E F; do ov POS_$p; step 02_pos_$p; done
ov POS_B
adb shell input keyevent KEYCODE_HOME; step 03_home
adb shell input tap 1097 1370; step 04_navi 3
adb shell am start --user 10 -a android.settings.SETTINGS >/dev/null; step 05_settings 3
$S/p.sh drive; step 06_drive_D30 3
adb shell dumpsys car_service --services CarUxRestrictionsManagerService | grep "Display id" | tee -a $F
TID=$(adb shell dumpsys activity activities | grep -E "Task\{.*com.probe.topbar" | grep -oE "#[0-9]+" | head -1 | tr -d '#')
echo "probe task id=$TID" | tee -a $F
adb shell am stack remove $TID; step 07_app_closed 3
adb shell dumpsys activity activities | grep -c "com.probe.topbar/.MainActivity" | sed 's/^/MainActivity records after close: /' | tee -a $F
sleep 10; step 08_app_closed_plus10s 1
adb logcat -d | grep -E "probe   :" | cut -c7-300 > T8_overlay_log.txt
adb logcat -d | grep -iE "probe|ActivityTaskManager|Background activity start|WindowManager" | cut -c7-300 > T8_full_log.txt
