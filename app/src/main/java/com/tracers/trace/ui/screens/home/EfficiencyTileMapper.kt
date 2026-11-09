package com.tracers.trace.ui.screens.home

import com.tracers.trace.domain.EfficiencySummary
import kotlin.math.abs

/**
 * 전비 계산 결과 → 홈 전비 타일. 순수 함수.
 *
 * - 값: 오늘 전비, 오늘 주행이 없으면 90일 평균
 * - 막대: 90일 평균을 눈금(50%)으로 두고 평균의 2배를 끝으로
 * - 모든 값은 앱 계산값이라 "추정"을 붙인다 (정차: 보조 줄, 주행: 상태 줄)
 */
object EfficiencyTileMapper {

    /** 기록이 없을 때. 지금까지의 빈 상태 그대로 */
    val Empty = TileState.Efficiency(
        value = "–",
        unit = "km/kWh",
        status = "데이터를 모으는 중이에요",
        statusLevel = StatusLevel.Normal,
        barPercent = 0f,
    )

    fun map(summary: EfficiencySummary?, isDriving: Boolean): TileState.Efficiency {
        val avg = summary?.averageKmPerKwh ?: return Empty
        val today = summary.todayKmPerKwh
        val diff = summary.todayDiffKmPerKwh
        val value = today ?: avg

        val (status, level) = when {
            today == null || diff == null -> (if (isDriving) "평균 ${fmt(avg)}" else "오늘 주행 전 · 90일 평균") to StatusLevel.Normal
            diff >= 0 -> (if (isDriving) "평소보다 좋아요" else "평균보다 ${fmt(abs(diff))} 좋아요") to StatusLevel.Good
            else -> (if (isDriving) "평소보다 낮아요" else "평균보다 ${fmt(abs(diff))} 낮아요") to StatusLevel.Caution
        }

        return TileState.Efficiency(
            value = fmt(value),
            unit = "km/kWh",
            status = if (isDriving) "추정 · $status" else status,
            statusLevel = level,
            barPercent = (value / (avg * 2) * 100).toFloat().coerceIn(0f, 100f),
            barMarkPercent = 50f,
            subText = buildList {
                add("추정")
                add(summary.hvacSharePercent?.let { "공조 $it%" } ?: "공조 —")
                add("회생 ${fmt(summary.regenKwh)} kWh")
            }.joinToString(" · "),
        )
    }

    private fun fmt(v: Double) = String.format("%.1f", v)
}
