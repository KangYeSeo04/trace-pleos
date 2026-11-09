package com.tracers.trace.domain

/**
 * 주행 1회를 묶어 요약을 만든다 (C-01).
 *
 * - 시작: [DriveState.DRIVING]이 되었고 진행 중인 주행이 없을 때
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

    val isRecording: Boolean get() = trip != null

    /** 차량 값이 바뀔 때마다 부른다. 주행 중이 아니면 마지막 값만 기억한다 */
    fun onSample(s: VehicleSample) {
        trip?.add(s)
        last = merge(last, s)
    }

    /** 주행 상태가 바뀔 때 부른다. 주행이 끝나 요약이 나오면 돌려준다 */
    fun onDriveState(state: DriveState, nowMs: Long): TripRecord? {
        return when (state) {
            DriveState.DRIVING -> {
                if (trip == null) trip = Accumulator(nowMs, last)
                null
            }
            DriveState.STOPPED -> null
            DriveState.PARKED -> {
                val t = trip ?: return null
                trip = null
                t.finish(nowMs).takeIf { it.distanceKm >= minDistanceKm }
            }
        }
    }

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
        private val startOdo = start?.odometerKm
        private val startPercent = start?.batteryPercent
        private var endOdo = startOdo
        private var endPercent = startPercent

        private var prev: VehicleSample? = start
        private var prevKwh = start?.batteryKwh
        private var usedKwh = 0.0
        private var regenKwh = 0.0
        private var integratedKm = 0.0
        private var hvacOnMs = 0L

        // 급가속: 1초 창의 시작점과, 한 번 센 뒤 가속이 풀릴 때까지 다시 세지 않는 플래그
        private var accelRefTime = start?.timeMs
        private var accelRefSpeed = start?.speedMps
        private var accelArmed = true
        private var hardAccel = 0

        fun add(raw: VehicleSample) {
            val p = prev
            val s = if (p == null) raw else merge(p, raw)

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
            val t0 = accelRefTime
            val v0 = accelRefSpeed
            if (t0 == null || v0 == null) {
                accelRefTime = t; accelRefSpeed = v; return
            }
            val dt = t - t0
            if (dt < ACCEL_WINDOW_MS) return
            if (dt <= MAX_GAP_MS) {
                val kmhPerSec = (v - v0) * 3.6 / (dt / 1000.0)
                // 차속이 Float(m/s)라 경계값이 10.9999처럼 나오므로 작은 오차를 허용
                if (kmhPerSec >= hardAccelKmhPerSec - EPSILON_KMH) {
                    if (accelArmed) hardAccel++
                    accelArmed = false
                } else {
                    accelArmed = true
                }
            }
            accelRefTime = t; accelRefSpeed = v
        }

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
        const val MIN_DISTANCE_KM = 0.1
        private const val EPSILON_KMH = 1e-3
        private const val ACCEL_WINDOW_MS = 1_000L
        /** 이보다 긴 샘플 간격은 끊긴 것으로 보고 거리 적분·가속 판단에서 뺀다 */
        private const val MAX_GAP_MS = 5_000L
    }
}
