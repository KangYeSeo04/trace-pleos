package com.tracers.trace.domain

/** 위험 운전 이벤트 4종 (E-01). 감점은 [DrivingThresholds]에 */
enum class DrivingEventType { HARD_ACCEL, HARD_DECEL, HARD_STOP, SPEEDING }

data class DrivingEvent(val type: DrivingEventType, val timeMs: Long)

/**
 * 차속으로 위험 운전 이벤트를 판정한다 (B-04, E-01, 전비 C-01의 급가속 횟수).
 * 판정 기준은 모두 [DrivingThresholds]. 앱 안에서 이 판정기 하나만 쓴다.
 *
 * - 차속 변화: 지금 값과 정확히 1초 전 값(그 시점에 유지되던 값)을 비교한다.
 *   차속이 바뀔 때만 오는 신호도 다음 값이 올 때까지 유지된다고 본다
 * - 급가속: 초당 +[DrivingThresholds.HARD_ACCEL_KMH_PER_SEC] 이상. 넘는 순간 1회, 기준 아래로 풀릴 때까지 다시 세지 않음
 * - 급감속·급정거: 감속이 초당 −[DrivingThresholds.HARD_DECEL_KMH_PER_SEC] 이하로 내려간 구간 하나를 1회로 본다.
 *   구간 안에서 −[DrivingThresholds.HARD_STOP_KMH_PER_SEC] 이하가 되면 그 순간 급정거로 세고,
 *   끝까지 거기에 못 미치면 구간이 끝날 때 급감속으로 센다 (둘을 겹쳐 세지 않음)
 * - 과속: 제한속도 + [DrivingThresholds.SPEEDING_MARGIN_KMH] 초과가 [DrivingThresholds.SPEEDING_HOLD_MS] 이어지면 1회.
 *   제한속도 정보가 없으면(null·0) 세지 않고 잰 시간도 지운다
 *
 * 입력은 [SpeedSpikeFilter]를 거친 값이어야 한다. 스레드 안전하지 않다.
 */
class DrivingEventDetector {
    private val history = ArrayDeque<SpeedReading>()

    private var accelArmed = true
    private var inDecel = false
    private var stopCounted = false

    private var speedLimitKmh: Int? = null
    private var overSinceMs: Long? = null
    private var speedingCounted = false

    /** NaviHelper 제한속도 (km/h). 0 이하·null = 정보 없음 */
    fun onSpeedLimit(kmh: Int?) {
        speedLimitKmh = kmh?.takeIf { it > 0 }
        if (speedLimitKmh == null) resetSpeeding()
    }

    /** 차속 하나를 넣고 이번에 생긴 이벤트를 돌려준다 */
    fun onSpeed(r: SpeedReading): List<DrivingEvent> {
        val out = ArrayList<DrivingEvent>(1)
        rate(r)?.let { checkRate(it, r.timeMs, out) }
        checkSpeeding(r, out)
        return out
    }

    /** 1초 전 값과 비교한 변화율 (km/h per 초). 1초 전 값을 아직 모르면 null */
    private fun rate(r: SpeedReading): Double? {
        history.addLast(r)
        val windowStart = r.timeMs - RATE_WINDOW_MS
        // 창 시작 시점에 유지되던 값(창 시작 이전의 마지막 값) 하나만 남긴다
        while (history.size >= 2 && history[1].timeMs <= windowStart) history.removeFirst()
        val anchor = history.first()
        if (anchor.timeMs > windowStart) return null
        return (r.kmh - anchor.kmh) / (RATE_WINDOW_MS / 1000.0)
    }

    private fun checkRate(rate: Double, t: Long, out: MutableList<DrivingEvent>) {
        // 차속이 Float(m/s)라 경계값이 10.9999처럼 나오므로 작은 오차를 허용
        if (rate >= DrivingThresholds.HARD_ACCEL_KMH_PER_SEC - EPSILON) {
            if (accelArmed) out += DrivingEvent(DrivingEventType.HARD_ACCEL, t)
            accelArmed = false
        } else {
            accelArmed = true
        }

        when {
            rate <= -DrivingThresholds.HARD_STOP_KMH_PER_SEC + EPSILON -> {
                inDecel = true
                if (!stopCounted) out += DrivingEvent(DrivingEventType.HARD_STOP, t)
                stopCounted = true
            }
            rate <= -DrivingThresholds.HARD_DECEL_KMH_PER_SEC + EPSILON -> inDecel = true
            else -> {
                if (inDecel && !stopCounted) out += DrivingEvent(DrivingEventType.HARD_DECEL, t)
                inDecel = false
                stopCounted = false
            }
        }
    }

    private fun checkSpeeding(r: SpeedReading, out: MutableList<DrivingEvent>) {
        val limit = speedLimitKmh
        if (limit == null || r.kmh <= limit + DrivingThresholds.SPEEDING_MARGIN_KMH + EPSILON) {
            resetSpeeding()
            return
        }
        val since = overSinceMs ?: r.timeMs.also { overSinceMs = it }
        if (!speedingCounted && r.timeMs - since >= DrivingThresholds.SPEEDING_HOLD_MS) {
            out += DrivingEvent(DrivingEventType.SPEEDING, r.timeMs)
            speedingCounted = true
        }
    }

    private fun resetSpeeding() {
        overSinceMs = null
        speedingCounted = false
    }

    private companion object {
        const val RATE_WINDOW_MS = 1_000L
        const val EPSILON = 1e-3
    }
}

/**
 * 거칠었음 (B-04): 급가속·급감속(급정거 포함)이 최근 [DrivingThresholds.ROUGH_WINDOW_MS]에
 * [DrivingThresholds.ROUGH_MIN_EVENTS]회 이상. 과속은 넣지 않는다.
 */
class RoughDrivingWindow {
    private val times = ArrayDeque<Long>()

    fun onEvent(e: DrivingEvent) {
        if (e.type != DrivingEventType.SPEEDING) times.addLast(e.timeMs)
    }

    fun isRough(nowMs: Long): Boolean {
        while (times.isNotEmpty() && times.first() < nowMs - DrivingThresholds.ROUGH_WINDOW_MS) times.removeFirst()
        return times.size >= DrivingThresholds.ROUGH_MIN_EVENTS
    }
}
