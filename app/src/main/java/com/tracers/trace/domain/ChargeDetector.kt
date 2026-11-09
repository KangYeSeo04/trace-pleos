package com.tracers.trace.domain

/**
 * 충전 1회를 감지한다 (C-01 충전 횟수, C-02).
 *
 * - 시작: 충전구가 열림
 * - 종료: 충전구가 닫히거나 주행이 시작됨
 * - 그 사이 배터리 %가 올랐을 때만 [ChargeRecord]를 낸다
 *
 * EV_CHARGE_STATE는 에뮬레이터에서 고정값만 와서 쓰지 않는다 (PLEOS_API_확인현황).
 * 스레드 안전하지 않다. 한 코루틴에서만 부른다.
 */
class ChargeDetector {

    private var portOpen: Boolean? = null
    private var lastPercent: Int? = null
    private var lastKwh: Double? = null
    private var session: Session? = null

    private class Session(val startMs: Long, var startPercent: Int?, var startKwh: Double?) {
        var endPercent: Int? = startPercent
        var endKwh: Double? = startKwh
    }

    /** 차량 값이 바뀔 때마다 부른다. 충전이 끝나면 기록을 돌려준다 */
    fun onSample(s: VehicleSample, capacityKwh: Double? = null): ChargeRecord? {
        s.batteryPercent?.let { lastPercent = it }
        s.batteryKwh?.let { lastKwh = it }

        session?.let { cur ->
            s.batteryPercent?.let { p ->
                if (cur.startPercent == null) cur.startPercent = p
                cur.endPercent = p
            }
            s.batteryKwh?.let { k ->
                if (cur.startKwh == null) cur.startKwh = k
                cur.endKwh = k
            }
        }

        val open = s.chargePortOpen ?: return null
        val wasOpen = portOpen
        portOpen = open

        return when {
            open && wasOpen != true && session == null -> {
                session = Session(s.timeMs, lastPercent, lastKwh)
                null
            }
            !open && session != null -> close(s.timeMs, capacityKwh)
            else -> null
        }
    }

    fun onDriveState(state: DriveState, nowMs: Long, capacityKwh: Double? = null): ChargeRecord? =
        if (state == DriveState.DRIVING && session != null) close(nowMs, capacityKwh) else null

    private fun close(endMs: Long, capacityKwh: Double?): ChargeRecord? {
        val cur = session ?: return null
        session = null
        val sp = cur.startPercent ?: return null
        val ep = cur.endPercent ?: return null
        if (ep <= sp) return null

        val sk = cur.startKwh
        val ek = cur.endKwh
        val added = when {
            sk != null && ek != null && ek > sk -> ek - sk
            capacityKwh != null -> capacityKwh * (ep - sp) / 100.0
            else -> 0.0
        }
        return ChargeRecord(
            startedAtMs = cur.startMs,
            endedAtMs = endMs,
            startPercent = sp,
            endPercent = ep,
            addedKwh = added,
        )
    }
}
