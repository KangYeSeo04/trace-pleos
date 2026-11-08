#!/bin/bash
B=http://127.0.0.1:8088
step(){ echo "STEP $1"; adb shell log -t PROBE "STEP $1"; curl -s -m 5 -X PUT "$B$2" | head -c 140; echo; sleep ${3:-3}; }
step "SOC 80" "/vehicle/ev-battery?value=80&field=soc"
step "배터리잔량 50kWh" "/vehicle/ev-battery?value=50&field=battery_level"
step "충전구 열림" "/vehicle/ev-battery?value=true&field=charge_port_open"
step "충전 스위치 켬" "/vehicle/ev-battery?value=true&field=charge_switch"
step "충전 전류제한 32A" "/vehicle/ev-battery?value=32&field=current_limit"
step "충전 스위치 끔" "/vehicle/ev-battery?value=false&field=charge_switch"
step "충전구 닫힘" "/vehicle/ev-battery?value=false&field=charge_port_open"
step "주행가능거리 250km" "/vehicle/odometer?remaining_range_m=250000"
step "크루즈 ready" "/vehicle/driving-mode?ad_mode_state=ready"
step "크루즈 목표 22.2m/s" "/vehicle/driving-mode?target_speed_mps=22.222"
step "크루즈 not_ready" "/vehicle/driving-mode?ad_mode_state=not_ready"
step "타이어 FL 180kPa" "/vehicle/tire-pressure?pressure_kpa=180&area=1114113"
step "공조 전원 켬 + 운전석 28도 + 풍량6" "/vehicle/hvac?power=true&driver_temp=28&fan_speed=6"
step "D 50km/h" "/vehicle/gear?gear=Drive" 1
step "속도 50" "/vehicle/controls?speed=50&accelerator=30&brake=0&steering=0" 8
step "정지" "/vehicle/controls?speed=0&accelerator=0&brake=0&steering=0" 2
step "Park" "/vehicle/gear?gear=Park" 2
step "공조 원복" "/vehicle/hvac?power=false&driver_temp=22&fan_speed=3" 2
step "SOC 100 원복" "/vehicle/ev-battery?value=100&field=soc" 2
