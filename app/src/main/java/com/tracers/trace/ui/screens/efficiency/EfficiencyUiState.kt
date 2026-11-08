package com.tracers.trace.ui.screens.efficiency

/**
 * 전비 상세 화면 상태 (S03).
 */
data class EfficiencyUiState(
    val isLoading: Boolean = false,
    val average90Days: EfficiencyAverage? = null,
    val chargeTiming: ChargeTiming? = null,
    // TODO(2회차): 달력, 월별 전비, 이번 달 예측
)

/**
 * 최근 90일 평균 전비.
 */
data class EfficiencyAverage(
    val avgEfficiency: Float,          // 예: 5.8
    val todayComparison: TodayComparison?,
    val summary: EfficiencySummary
)

/**
 * 오늘 전비 비교.
 */
data class TodayComparison(
    val todayValue: Float,      // 예: 6.4
    val difference: Float,       // 예: 1.1
    val isAboveAverage: Boolean  // true: 평균보다 높음, false: 낮음
)

/**
 * 전비 요약 4칸.
 */
data class EfficiencySummary(
    val regenBraking: Float,  // 회생제동 kWh
    val hvac: Int,            // 공조 %
    val hardAccel: Int,       // 급가속 횟수
    val charges: Int          // 충전 횟수
)

/**
 * 충전 타이밍.
 */
data class ChargeTiming(
    val title: String,        // 예: "목요일까지 충분해요"
    val description: String,  // 예: "배터리 64% · 하루 평균 34 km 기준"
    val forecast: List<ChargeForecastDay>
)

/**
 * 충전 예측 일별.
 */
data class ChargeForecastDay(
    val label: String,    // "지금", "내일", "수", "목", "금"
    val percentage: Int,  // 64, 53, 42, 31, 19
    val isToday: Boolean  // 오늘만 파란색
)
