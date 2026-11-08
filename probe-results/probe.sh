#!/bin/bash
# usage: p.sh <cmd> [args]
R=/Users/gang-yeseo/AndroidStudioProjects/trace/probe-results
C=http://127.0.0.1:8088
case "$1" in
  park)  curl -s -m5 -X PUT "$C/vehicle/controls?speed=0&accelerator=0&brake=100&steering=0" >/dev/null; curl -s -m5 -X PUT "$C/vehicle/gear?gear=Park" >/dev/null;;
  drive) curl -s -m5 -X PUT "$C/vehicle/gear?gear=Drive" >/dev/null; curl -s -m5 -X PUT "$C/vehicle/controls?speed=30&accelerator=0&brake=0&steering=0" >/dev/null;;
  shot)  adb exec-out screencap -p > "$R/$2.png"; echo "saved $2.png";;
  notif) adb shell dumpsys notification --noredact | grep -A 30 com.probe.topbar > "$R/$2_notif.txt"; wc -l < "$R/$2_notif.txt";;
  log)   adb logcat -d | grep -iE "probe|NotificationService|ActivityTaskManager|Background activity start|WindowManager" > "$R/$2_log.txt"; wc -l < "$R/$2_log.txt";;
  b)     shift; adb shell am broadcast --user 10 -n com.probe.topbar/.CmdReceiver -a "$@" >/dev/null;;
  svc)   shift; a=$1; shift; adb shell am start-foreground-service --user 10 -n com.probe.topbar/.ProbeService -a "$a" "$@";;
  ux)    adb shell dumpsys car_service --services CarUxRestrictionsManagerService 2>/dev/null | grep -iE "Current UXR|isRequiresDistractionOptimization|mActiveRestrictions|Restrictions" | head -5;;
esac
