# 주행 모드 전환 테스트

에뮬레이터에서 정차 → 주행 전환 시 레이아웃 높이가 유지되는지 확인하는 방법입니다.

## 1. 앱 실행 및 로그 초기화

```bash
./gradlew installDebug
adb logcat -c
adb shell am force-stop com.tracers.trace
adb shell am start -n com.tracers.trace/.MainActivity
```

## 2. 정차 모드 높이 측정

앱이 시작되면 (P 모드):

```bash
adb logcat -d -s HomeLayout:D | grep "mode: parked" | tail -10
```

예상 결과:
```
HeroStage height: ~365dp
TileGrid height: 390dp (고정)
Tile Efficiency height: ~154dp (padding 제외 시 190dp)
Tile Maintenance height: ~154dp
Tile Coin height: ~154dp
Tile Region height: ~154dp
```

## 3. 주행 모드로 전환

Virtual Console에서:
1. Gear → DRIVE
2. Speed → 30 km/h (8.33 m/s)
3. 2초 대기 (DriveStateDetector 지연)

## 4. 주행 모드 높이 측정

```bash
adb logcat -d -s HomeLayout:D | grep "mode: driving" | tail -10
```

예상 결과 (정차 모드와 **같은 높이**):
```
HeroStage height: ~365dp
TileGrid height: 390dp (고정)
Tile Efficiency height: ~154dp
Tile Maintenance height: ~154dp
Tile Coin height: ~154dp
Tile Region height: ~154dp
```

## 5. 비교

정차 모드와 주행 모드의 높이가 **동일**하면 성공.

높이가 다르면:
- TileGrid Box의 고정 높이(390dp) 확인
- Tile의 fillMaxSize() 적용 확인
- Crossfade가 타일 내용에만 적용되는지 확인
