package com.tracers.trace.data.vehicle

import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 신호 인터페이스. 화면과 계산 로직은 이 인터페이스만 사용하고 SDK를 직접 부르지 않는다.
 *
 * 지원 범위 (docs/PLEOS_API_확인현황.md 기준):
 * - 속도: AAOS PERF_VEHICLE_SPEED (100ms 간격)
 * - 기어: AAOS GEAR_SELECTION (P/R/N/D). Vehicle SDK Gear는 deprecated라 쓰지 않음
 * - 브레이크: Vehicle SDK Brake (Boolean on/off만, % 아님)
 * - 조향각: Vehicle SDK SteeringWheel (부호 보정 필요: 콘솔 +90 → SDK -90)
 * - 방향지시등: Vehicle SDK TurnSignal (OFF/LEFT/RIGHT/HAZARD)
 * - EV 배터리 %: Vehicle SDK EvBattery.getEvBatteryStateOfCharge
 * - EV 배터리 잔량(Wh): Vehicle SDK EvBattery.getEvBatteryLevel
 * - EV 주행가능거리(m): Vehicle SDK Odometer.getRangeRemainingDistance
 * - 충전구 열림: Vehicle SDK EvBattery.isEvChargePortOpen
 * - 총 주행거리(km): Vehicle SDK Odometer.getCurrentDistanceDriven
 * - EV 배터리 용량(Wh): Vehicle SDK EvBattery.getEvBatteryCapacity (시작 시 1회)
 * - 공조 AC 켜짐: Vehicle SDK HVAC.getHvacAcState (지원 영역 중 하나라도 켜지면 true)
 */
interface VehicleSignals {

    /** 차량 속도 (m/s). km/h로 변환: speedMps * 3.6 */
    val speedMps: StateFlow<Float?>

    /** 기어 상태 */
    val gear: StateFlow<Gear?>

    /** 브레이크 밟음 여부 (true = 밟음). 밟은 정도(%)는 지원되지 않음 */
    val brakePedal: StateFlow<Boolean?>

    /**
     * 조향각 (도). 양수 = 오른쪽, 음수 = 왼쪽.
     * 주의: SDK 원본 값은 부호가 반대임. 구현체에서 보정.
     */
    val steeringAngle: StateFlow<Float?>

    /** 방향지시등 상태 */
    val turnSignal: StateFlow<TurnSignal?>

    /** EV 배터리 충전 상태 (0~100%) */
    val batteryPercent: StateFlow<Int?>

    /** EV 배터리 잔량 (Wh) */
    val batteryLevelWh: StateFlow<Float?>

    /** 남은 주행 가능 거리 (m) */
    val rangeRemainingM: StateFlow<Float?>

    /** 충전구 열림 여부 */
    val chargePortOpen: StateFlow<Boolean?>

    /** 총 주행 거리 (km). 이번 주행 거리는 앱에서 시작 시점과의 차이로 계산 */
    val odometerKm: StateFlow<Float?>

    /** EV 배터리 전체 용량 (Wh). 잔량 kWh 대체 계산(용량 × %)에도 쓴다 */
    val batteryCapacityWh: StateFlow<Float?>

    /** 공조(에어컨) 켜짐 여부 */
    val hvacAcOn: StateFlow<Boolean?>

    /** SDK 연결 시작 */
    fun start()

    /** SDK 연결 해제 */
    fun stop()
}

enum class Gear {
    PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN
}

enum class TurnSignal {
    OFF, LEFT, RIGHT, HAZARD
}
