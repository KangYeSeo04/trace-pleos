package com.tracers.trace.domain

import com.tracers.trace.domain.DrivingThresholds as T

/** 안전점수 (E-01). 기준값은 [DrivingThresholds] */
object SafetyScorer {
    const val START = T.SAFETY_START

    /** 이번 1km 점수: 100에서 이벤트마다 감점, 최소 0 */
    fun kmScore(c: EventCounts): Int {
        val penalty = c.hardAccel * T.PENALTY_HARD_ACCEL +
            c.hardDecel * T.PENALTY_HARD_DECEL +
            c.hardStop * T.PENALTY_HARD_STOP +
            c.speeding * T.PENALTY_SPEEDING
        return (T.KM_SCORE_CLEAN - penalty).coerceAtLeast(0)
    }

    /** 안전점수 = 기존 × 0.98 + 이번 1km × 0.02 */
    fun next(prev: Double, kmScore: Int): Double =
        prev * (1 - T.SAFETY_KM_WEIGHT) + kmScore * T.SAFETY_KM_WEIGHT
}
