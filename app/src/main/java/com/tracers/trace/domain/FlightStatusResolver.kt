package com.tracers.trace.domain

/**
 * 기체 상태 중 주행 중 3종 (B-03). 우선순위 거칠었음 > 매끄럽게·천천히.
 * TODO(B-03, C-02, D-02): 충전 > 점검 임박은 해당 기능이 생기면 앞에 붙인다. 쉬는 중은 정차 문구에서.
 */
enum class FlightStatus { ROUGH, SMOOTH, SLOW }

/**
 * 주행 상태·차속·거칠었음 → [FlightStatus]. 천천히/매끄럽게는 기준 근처에서 깜빡이지 않게
 * [DrivingThresholds.SLOW_BELOW_KMH] 아래로 내려가야 천천히, [DrivingThresholds.SMOOTH_FROM_KMH] 이상이어야
 * 매끄럽게로 바뀐다. 처음 값은 둘의 가운데로 정한다. 스레드 안전하지 않다.
 */
class FlightStatusResolver {
    private var slow: Boolean? = null

    /** 주행 중이 아니면 null */
    fun resolve(state: DriveState, speedKmh: Double, rough: Boolean): FlightStatus? {
        if (state != DriveState.DRIVING) {
            slow = null
            return null
        }
        val wasSlow = slow
        val isSlow = when {
            wasSlow == null -> speedKmh < (DrivingThresholds.SLOW_BELOW_KMH + DrivingThresholds.SMOOTH_FROM_KMH) / 2
            wasSlow -> speedKmh < DrivingThresholds.SMOOTH_FROM_KMH
            else -> speedKmh < DrivingThresholds.SLOW_BELOW_KMH
        }
        slow = isSlow
        return when {
            rough -> FlightStatus.ROUGH
            isSlow -> FlightStatus.SLOW
            else -> FlightStatus.SMOOTH
        }
    }
}
