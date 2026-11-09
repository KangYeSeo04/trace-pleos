package com.tracers.trace.domain

/**
 * 주행 1회를 묶어 요약을 만든다 (C-01).
 *
 * - 시작: [DriveState.DRIVING]이 되었고 진행 중인 주행이 없을 때. 판정은 2초 유지 뒤라서,
 *   차가 움직이기 시작한 순간부터 후보로 쌓아 두었다가 DRIVING이 되면 그 후보를 주행으로 쓴다
 *   (출발 가속·거리·에너지가 빠지지 않게). 판정 전에 다시 멈추면 후보는 버린다
 * - 종료: [DriveState.PARKED]. 신호등 정차([DriveState.STOPPED])는 주행을 끝내지 않는다
 * - 원시 샘플은 누적값만 갱신하고 버린다. 저장되는 건 [TripRecord] 하나
 *
 * 스레드 안전하지 않다. 한 코루틴에서만 부른다.
 */
class TripRecorder(
    /** 급가속 기준 (km/h per 초). 기획안 B-04 */
    private val hardAccelKmhPerSec: Double = DrivingThresholds.HARD_ACCEL_KMH_PER_SEC,
    /** 이보다 짧은 주행은 버린다 (주차장 이동 등) */
    private val minDistanceKm: Double = MIN_DISTANCE_KM,
) {
    private var last: VehicleSample? = null
    private var trip: Accumulator? = null
    /** 움직이기 시작했지만 아직 DRIVING 판정 전인 후보 */
    private var candidate: Accumulator? = null

    val isRecording: Boolean get() = trip != null

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
            hvacAcOn = s.hvacAcOn ?: prev.hvacAcOn,
            chargePortOpen = s.chargePortOpen ?: prev.chargePortOpen,
        )
    }

    private inner class Accumulator(private val startMs: Long, start: VehicleSample?) {
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

        // 급가속: 차속은 다음 값이 올 때까지 유지된다고 보고(바뀔 때만 오는 신호 대응)
        // 지금 값과 정확히 1초 전 값을 비교한다. 1초 창 + 그 직전 값 하나만 들고 있는다
        private val speedHistory = ArrayDeque<Pair<Long, Float>>().apply {
            val t = start?.timeMs
            val v = start?.speedMps
            if (t != null && v != null) addLast(t to v)
        }
        // 한 번 센 뒤 가속이 풀릴 때까지 다시 세지 않는다
        private var accelArmed = true
        private var hardAccel = 0

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
                    if (p.hvacAcOn == true) hvacOnMs += dt
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

            s.speedMps?.let { v -> checkAccel(s.timeMs, v) }
            s.odometerKm?.let { endOdo = it }
            s.batteryPercent?.let { endPercent = it }
            prev = s
        }

        private fun checkAccel(t: Long, v: Float) {
            speedHistory.addLast(t to v)
            val windowStart = t - ACCEL_WINDOW_MS
            while (speedHistory.size >= 2 && speedHistory[1].first <= windowStart) speedHistory.removeFirst()
            val (anchorTime, anchorSpeed) = speedHistory.first()
            if (anchorTime > windowStart) return // 1초 전 값을 아직 모름

            val kmhPerSec = (v - anchorSpeed) * 3.6 / (ACCEL_WINDOW_MS / 1000.0)
            // 차속이 Float(m/s)라 경계값이 10.9999처럼 나오므로 작은 오차를 허용
            if (kmhPerSec >= hardAccelKmhPerSec - EPSILON_KMH) {
                if (accelArmed) hardAccel++
                accelArmed = false
            } else {
                accelArmed = true
            }
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
        private const val EPSILON_KMH = 1e-3
        private const val ACCEL_WINDOW_MS = 1_000L
        /** 이보다 긴 샘플 간격은 끊긴 것으로 보고 거리 적분에서 뺀다 */
        private const val MAX_GAP_MS = 5_000L
    }
}
