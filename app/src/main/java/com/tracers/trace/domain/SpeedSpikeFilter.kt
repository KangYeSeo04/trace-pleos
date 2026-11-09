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
 * - 차속은 100ms마다 같은 값이 되풀이되어 오므로, 튄 값이 연달아 여러 번 와도 같은 값이면 하나로 본다
 *   (콘솔 실측: 36 → 28, 28, 28 → 35)
 * - 직전에 통과한 값과 [DrivingThresholds.SPIKE_MIN_JUMP_KMH] 이상 차이 나는 값만 잡아 둔다.
 *   그 밖의 값은 바로 통과한다 (평소에는 지연 없음)
 * - 잡아 둔 값은 처음 온 때부터 그 시간 안에 같은 쪽에서 절반 넘게 되돌아오면 버리고,
 *   아니면(그대로 머물거나 시간이 지나면) 원래 시각으로 내보낸다
 *
 * 스레드 안전하지 않다. 한 코루틴에서만 부른다.
 */
class SpeedSpikeFilter(
    private val windowMs: Long = DrivingThresholds.SPIKE_WINDOW_MS,
    private val minJumpKmh: Double = DrivingThresholds.SPIKE_MIN_JUMP_KMH,
) {
    private var confirmed: SpeedReading? = null
    /** 잡아 둔 튐 후보 (같은 값의 되풀이) */
    private val pending = ArrayList<SpeedReading>(4)

    /** 값 하나를 넣고, 이번에 확정된 값들을 시간 순으로 돌려준다 */
    fun push(n: SpeedReading): List<SpeedReading> {
        val c = confirmed ?: return listOf(n).also { confirmed = n }
        val out = ArrayList<SpeedReading>(2)
        if (pending.isNotEmpty()) {
            val p0 = pending.first()
            val inWindow = n.timeMs - p0.timeMs <= windowMs
            when {
                inWindow && abs(n.kmh - p0.kmh) < SAME_KMH -> {
                    pending += n
                    return out
                }
                inWindow && isSpike(c, p0, n) -> pending.clear()
                else -> {
                    out += pending
                    confirmed = pending.last()
                    pending.clear()
                }
            }
        }
        val base = confirmed!!
        if (abs(n.kmh - base.kmh) >= minJumpKmh) {
            pending += n
        } else {
            out += n
            confirmed = n
        }
        return out
    }

    /** 잡아 둔 값이 있으면 내보낸다 (주행 끝 등) */
    fun flush(): List<SpeedReading> {
        if (pending.isEmpty()) return emptyList()
        val out = pending.toList()
        confirmed = out.last()
        pending.clear()
        return out
    }

    /** c(직전 확정) → p(잡아 둔 값) → n(다음 값): p만 튀고 n이 c 쪽으로 절반 넘게 돌아왔나 */
    private fun isSpike(c: SpeedReading, p: SpeedReading, n: SpeedReading): Boolean {
        val up = p.kmh - c.kmh
        val back = p.kmh - n.kmh
        return up.sign == back.sign && abs(n.kmh - c.kmh) <= abs(up) / 2
    }

    private companion object {
        /** 이 차이 안이면 같은 값의 되풀이로 본다 (km/h) */
        const val SAME_KMH = 0.05
    }
}
