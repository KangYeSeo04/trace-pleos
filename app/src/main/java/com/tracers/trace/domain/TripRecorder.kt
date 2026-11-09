package com.tracers.trace.domain

/**
 * 주행 1회를 묶어 요약을 만든다 (C-01).
 *
 * - 시작: [DriveState.DRIVING]이 되었고 진행 중인 주행이 없을 때. 판정은 2초 유지 뒤라서,
 *   차가 움직이기 시작한 순간부터 후보로 쌓아 두었다가 DRIVING이 되면 그 후보를 주행으로 쓴다
 *   (출발 가속·거리·에너지가 빠지지 않게). 판정 전에 다시 멈추면 후보는 버린다
 * - 종료: [DriveState.PARKED]. 신호등 정차([DriveState.STOPPED])는 주행을 끝내지 않는다
 * - 원시 샘플은 누적값만 갱신하고 버린다. 저장되는 건 [TripRecord] 하나
 * - 급가속 횟수는 [DrivingEventDetector]가 낸 이벤트를 [onDrivingEvent]로 받아 센다 (판정기는 앱에 하나)
 *
 * 스레드 안전하지 않다. 한 코루틴에서만 부른다.
 */
class TripRecorder(
    /** 이보다 짧은 주행은 버린다 (주차장 이동 등) */
    private val minDistanceKm: Double = MIN_DISTANCE_KM,
) {
    private var last: VehicleSample? = null
    private var trip: Accumulator? = null
    /** 움직이기 시작했지만 아직 DRIVING 판정 전인 후보 */
    private var candidate: Accumulator? = null

    val isRecording: Boolean get() = trip != null

    /** 진행 중인 주행의 시작 시각. 주행 중이 아니면 null */
    val currentStartMs: Long? get() = trip?.startMs

    /** 위험 운전 이벤트. 급가속만 전비 요약에 센다 (출발 판정 전 후보 구간 포함) */
    fun onDrivingEvent(e: DrivingEvent) {
        if (e.type == DrivingEventType.HARD_ACCEL) (trip ?: candidate)?.let { it.hardAccel++ }
    }

    /** 차량 값이 바뀔 때마다 부른다. 주행 중이 아니면 마지막 값만 기억한다 */
    fun onSample(s: VehicleSample) {
        val t = trip
        if (t != null) {
            t.add(s)
        } else {
            val speed = s.speedMps ?: last?.speedMps
            when {
                speed == null -> Unit
                speed > 0f -> (candidate ?: Accumulator(s.timeMs, last).also { candidate = it }).add(s)
                else -> candidate = null
            }
        }
        last = merge(last, s)
    }

    /** 주행 상태가 바뀔 때 부른다. 주행이 끝나 요약이 나오면 돌려준다 */
    fun onDriveState(state: DriveState, nowMs: Long): TripRecord? {
        return when (state) {
            DriveState.DRIVING -> {
                if (trip == null) trip = candidate ?: Accumulator(nowMs, last)
                candidate = null
                null
            }
            DriveState.STOPPED -> null
            DriveState.PARKED -> {
                candidate = null
                val t = trip ?: return null
                trip = null
                t.finish(nowMs).takeIf { it.distanceKm >= minDistanceKm }
            }
        }
    }

    /**
     * 진행 중인 주행의 지금까지 요약 (끝 시각 = 마지막으로 받은 값의 시각).
     * 앱이 도중에 꺼져도 마무리할 수 있게 주기적으로 저장하는 용도. 주행 중이 아니면 null
     */
    fun snapshot(): TripRecord? = trip?.snapshot()

    /** null인 값은 이전 값으로 채운다 (신호마다 갱신 주기가 다르다) */
    private fun merge(prev: VehicleSample?, s: VehicleSample): VehicleSample {
        if (prev == null) return s
        return VehicleSample(
            timeMs = s.timeMs,
            speedMps = s.speedMps ?: prev.speedMps,
            batteryKwh = s.batteryKwh ?: prev.batteryKwh,
            batteryPercent = s.batteryPercent ?: prev.batteryPercent,
            odometerKm = s.odometerKm ?: prev.odometerKm,
            hvacOn = s.hvacOn ?: prev.hvacOn,
            chargePortOpen = s.chargePortOpen ?: prev.chargePortOpen,
        )
    }

    private inner class Accumulator(val startMs: Long, start: VehicleSample?) {
        // 앱이 켜지자마자 움직여 시작 기준값이 없으면 첫 샘플을 기준으로 쓴다
        private var startOdo = start?.odometerKm
        private var startPercent = start?.batteryPercent
        private var endOdo = startOdo
        private var endPercent = startPercent

        private var prev: VehicleSample? = start
        private var prevKwh = start?.batteryKwh
        private var usedKwh = 0.0
        private var regenKwh = 0.0
        private var integratedKm = 0.0
        private var hvacOnMs = 0L

        var hardAccel = 0

        fun add(raw: VehicleSample) {
            val p = prev
            val s = if (p == null) raw else merge(p, raw)
            if (p == null) {
                if (startOdo == null) startOdo = s.odometerKm
                if (startPercent == null) startPercent = s.batteryPercent
            }

            if (p != null) {
                val dt = s.timeMs - p.timeMs
                if (dt > 0) {
                    // 시작 기준 샘플이 시작 시각보다 앞설 수 있으므로 시작 이후 구간만 센다
                    if (p.hvacOn == true) hvacOnMs += s.timeMs - maxOf(p.timeMs, startMs).coerceAtMost(s.timeMs)
                    val v = p.speedMps
                    if (v != null && dt <= MAX_GAP_MS) integratedKm += v * dt / 1_000_000.0
                }
            }

            s.batteryKwh?.let { kwh ->
                prevKwh?.let { pk ->
                    val d = kwh - pk
                    if (d < 0) usedKwh += -d else regenKwh += d
                }
                prevKwh = kwh
            }

            s.odometerKm?.let { endOdo = it }
            s.batteryPercent?.let { endPercent = it }
            prev = s
        }

        fun snapshot(): TripRecord = finish(prev?.timeMs?.coerceAtLeast(startMs) ?: startMs)

        fun finish(endMs: Long): TripRecord {
            val so = startOdo
            val eo = endOdo
            val distance = if (so != null && eo != null && eo >= so) eo - so else integratedKm
            return TripRecord(
                startedAtMs = startMs,
                endedAtMs = endMs,
                distanceKm = distance,
                energyUsedKwh = usedKwh,
                hardAccelCount = hardAccel,
                regenKwh = regenKwh,
                hvacOnSec = hvacOnMs / 1000,
                startBatteryPercent = startPercent,
                endBatteryPercent = endPercent,
            )
        }
    }

    companion object {
        /** 앱이 다시 켜졌을 때 남아 있던 스냅샷을 주행 1회로 마무리. 너무 짧으면 버린다 */
        fun finalizeRecovered(snapshot: TripRecord, minDistanceKm: Double = MIN_DISTANCE_KM): TripRecord? =
            snapshot.takeIf { it.distanceKm >= minDistanceKm }

        const val MIN_DISTANCE_KM = 0.1
        /** 이보다 긴 샘플 간격은 끊긴 것으로 보고 거리 적분에서 뺀다 */
        private const val MAX_GAP_MS = 5_000L
    }
}
