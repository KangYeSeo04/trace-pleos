package com.tracers.trace.domain

/** 1km 구간 안의 이벤트 횟수 */
data class EventCounts(
    val hardAccel: Int = 0,
    val hardDecel: Int = 0,
    val hardStop: Int = 0,
    val speeding: Int = 0,
) {
    /** 이벤트 없는 1km = 깨끗한 1km */
    val isClean: Boolean get() = hardAccel == 0 && hardDecel == 0 && hardStop == 0 && speeding == 0

    operator fun plus(type: DrivingEventType): EventCounts = when (type) {
        DrivingEventType.HARD_ACCEL -> copy(hardAccel = hardAccel + 1)
        DrivingEventType.HARD_DECEL -> copy(hardDecel = hardDecel + 1)
        DrivingEventType.HARD_STOP -> copy(hardStop = hardStop + 1)
        DrivingEventType.SPEEDING -> copy(speeding = speeding + 1)
    }
}

/**
 * 1km 구간 하나 (E-01, E-03 별 1칸).
 *
 * @property index 이번 주행에서 몇 번째 km인지 (1부터)
 */
data class KmSegment(
    val index: Int,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val counts: EventCounts,
)

/**
 * 주행을 1km 단위로 나눈다. 거리는 걸러진 차속을 적분한다 (차속은 다음 값이 올 때까지 유지된다고 봄).
 * 총 주행거리(Odometer)는 갱신 주기를 모르고 km 단위 Float라, 1km 경계를 정하는 데는 차속을 쓴다.
 *
 * - 신호가 [MAX_GAP_MS]보다 오래 끊긴 간격은 거리에 넣지 않는다
 * - 1km를 넘은 나머지 거리는 다음 구간으로 넘긴다
 * - 이벤트는 들어온 시점에 달리던 구간에 붙는다
 * - 주행이 끝나면 [reset]: 덜 찬 구간은 점수 없이 버린다
 *
 * 스레드 안전하지 않다.
 */
class KmSegmenter(
    private val segmentM: Double = DrivingThresholds.SEGMENT_KM * 1000.0,
) {
    private var prev: SpeedReading? = null
    private var meters = 0.0
    private var segStartMs: Long? = null
    private var counts = EventCounts()
    private var index = 0

    /** 차속 하나. 이번 값으로 1km가 차면 그 구간을 돌려준다 */
    fun onSpeed(r: SpeedReading): KmSegment? {
        val p = prev
        prev = r
        if (segStartMs == null) segStartMs = r.timeMs
        if (p == null) return null
        val dt = r.timeMs - p.timeMs
        if (dt <= 0 || dt > MAX_GAP_MS) return null
        meters += p.speedMps.coerceAtLeast(0f) * dt / 1000.0
        if (meters < segmentM) return null

        index++
        val seg = KmSegment(index, segStartMs!!, r.timeMs, counts)
        meters -= segmentM
        segStartMs = r.timeMs
        counts = EventCounts()
        return seg
    }

    fun onEvent(e: DrivingEvent) {
        counts += e.type
    }

    /** 주행 끝: 덜 찬 구간·번호를 버린다 */
    fun reset() {
        prev = null
        meters = 0.0
        segStartMs = null
        counts = EventCounts()
        index = 0
    }

    private companion object {
        const val MAX_GAP_MS = 5_000L
    }
}
