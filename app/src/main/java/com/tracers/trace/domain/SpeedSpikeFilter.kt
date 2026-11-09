package com.tracers.trace.domain

import kotlin.math.abs
import kotlin.math.sign

/** 차속 한 번 (이벤트 판정용). 값을 건너뛰지 않고 하나씩 받는다 */
data class SpeedReading(
    /** 측정 시각 (epoch ms) */
    val timeMs: Long,
    /** 차속 (m/s) */
    val speedMps: Float,
) {
    val kmh: Double get() = speedMps * 3.6
}

/**
 * 차속 잡음 거르기. [DrivingThresholds.SPIKE_WINDOW_MS] 안에 튀었다가 원래 값 근처로 돌아오는
 * 값 **하나**는 버린다 (예: 20 → 55 → 19 에서 55).
 *
 * - 직전에 통과한 값과 [DrivingThresholds.SPIKE_MIN_JUMP_KMH] 이상 차이 나는 값만 다음 값이 올 때까지 잡아 둔다.
 *   그 밖의 값은 바로 통과한다 (평소에는 지연 없음)
 * - 잡아 둔 값은 다음 값이 그 시간 안에 같은 쪽에서 절반 넘게 되돌아오면 버리고, 아니면 원래 시각으로 내보낸다
 *
 * 스레드 안전하지 않다. 한 코루틴에서만 부른다.
 */
class SpeedSpikeFilter(
    private val windowMs: Long = DrivingThresholds.SPIKE_WINDOW_MS,
    private val minJumpKmh: Double = DrivingThresholds.SPIKE_MIN_JUMP_KMH,
) {
    private var confirmed: SpeedReading? = null
    private var pending: SpeedReading? = null

    /** 값 하나를 넣고, 이번에 확정된 값들을 시간 순으로 돌려준다 (0~2개) */
    fun push(n: SpeedReading): List<SpeedReading> {
        val c = confirmed ?: return listOf(n).also { confirmed = n }
        val out = ArrayList<SpeedReading>(2)
        val p = pending
        if (p != null) {
            pending = null
            if (!isSpike(c, p, n)) {
                out += p
                confirmed = p
            }
        }
        val base = confirmed!!
        if (abs(n.kmh - base.kmh) >= minJumpKmh) {
            pending = n
        } else {
            out += n
            confirmed = n
        }
        return out
    }

    /** 잡아 둔 값이 있으면 내보낸다 (주행 끝 등) */
    fun flush(): List<SpeedReading> {
        val p = pending ?: return emptyList()
        pending = null
        confirmed = p
        return listOf(p)
    }

    /** c(직전 확정) → p(잡아 둔 값) → n(다음 값): p만 튀고 n이 시간 안에 c 쪽으로 절반 넘게 돌아왔나 */
    private fun isSpike(c: SpeedReading, p: SpeedReading, n: SpeedReading): Boolean {
        if (n.timeMs - p.timeMs > windowMs) return false
        val up = p.kmh - c.kmh
        val back = p.kmh - n.kmh
        return up.sign == back.sign && abs(n.kmh - c.kmh) <= abs(up) / 2
    }
}
