#!/bin/bash
B=http://localhost:8088
step(){ echo "$(date +%s%3N) STEP $1" | tee -a $LOG; adb shell log -t PROBE "STEP $1" ; curl -s -m 5 -X PUT "$B$2" | head -c 160; echo; sleep ${3:-3}; }
LOG=$1; : > $LOG
step "기어 Drive" "/vehicle/gear?gear=Drive" 2
step "속도 30" "/vehicle/controls?speed=30&accelerator=20&brake=0&steering=0" 4
step "조향 90" "/vehicle/controls?speed=30&accelerator=20&brake=0&steering=90" 3
step "브레이크 50" "/vehicle/controls?speed=20&accelerator=0&brake=50&steering=90" 3
step "브레이크 100" "/vehicle/controls?speed=10&accelerator=0&brake=100&steering=0" 3
step "방향지시등 왼쪽" "/vehicle/turn-signal?mode=BlinkLeft" 3
step "방향지시등 끔" "/vehicle/turn-signal?mode=Off" 2
step "에어컨 끔" "/vehicle/hvac?ac=false" 3
step "에어컨 켬" "/vehicle/hvac?ac=true" 3
step "와이퍼 앞 Low" "/vehicle/body?front_wiper=Low" 3
step "와이퍼 앞 Off" "/vehicle/body?front_wiper=Off" 2
step "주행거리 12345km" "/vehicle/odometer?traveled_distance_km=12345" 3
step "주행시간 42분" "/vehicle/odometer?usage_time_min=42" 3
step "경고등 엔진오일압력 On" "/vehicle/telltales/simulator?state=On&type=EngineOilPressure&color=Red" 3
step "경고등 엔진오일압력 Off" "/vehicle/telltales/simulator?state=Off&type=EngineOilPressure&color=Red" 2
step "정지" "/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0" 2
step "기어 Park" "/vehicle/gear?gear=Park" 2
