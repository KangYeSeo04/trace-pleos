package com.tracers.trace.ui.screens.efficiency

/**
 * 전비 상세 화면 상태 (S03).
 */
data class EfficiencyUiState(
    val isLoading: Boolean = false,
    val average90Days: EfficiencyAverage? = null,
    val chargeTiming: ChargeTiming? = null,
    val calendar: DrivingCalendar? = null,
    val monthlyEfficiency: MonthlyEfficiency? = null,
    val prediction: MonthPrediction? = null
)

/**
 * 최근 90일 평균 전비.
 */
data class EfficiencyAverage(
    val avgEfficiency: Float,          // 예: 5.8
    val todayComparison: TodayComparison?,
    val summary: EfficiencySummary,
    /** 앱이 잔량 감소량 ÷ 거리로 계산한 값이면 true → "추정" 표시 */
    val isEstimated: Boolean = false
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
    val hvac: Int?,           // 공조 %. null이면 "—" (공조 신호 미지원)
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

// ===== 주행 달력 =====

/**
 * 주행 기록 달력.
 */
data class DrivingCalendar(
    val year: Int,
    val month: Int,  // 1~12
    val days: List<CalendarDay>,
    val selectedDay: Int?,  // 선택된 날짜 (1~31)
    val selectedDayDetail: DayDetail?  // 선택 시 상세 정보
)

/**
 * 달력의 날짜 셀.
 */
data class CalendarDay(
    val dayOfMonth: Int,           // 1~31
    val hasDriving: Boolean,       // 주행 기록 있음 (회색 점)
    val hasCharging: Boolean,      // 충전 기록 있음 (파랑 점)
    val isFuture: Boolean,         // 미래 날짜 (비활성)
    val isToday: Boolean           // 오늘
)

/**
 * 선택된 날짜의 상세 정보.
 */
data class DayDetail(
    val dateLabel: String,          // "11월 2일 월요일"
    val durationMinutes: Int,       // 58
    val distanceKm: Float,          // 42.6
    val efficiency: Float,          // 6.4
    val efficiencyDiff: Float,      // +1.1
    val isAboveAverage: Boolean,
    val chargingRecord: ChargingRecord?
)

/**
 * 충전 기록.
 */
data class ChargingRecord(
    val timeRange: String,      // "08:10 – 08:52"
    val isFastCharging: Boolean,
    val stationName: String,    // "[충전소 이름]"
    val startPercent: Int,      // 31
    val endPercent: Int,        // 64
    val addedKwh: Float         // 28
)

// ===== 월별 전비 =====

/**
 * 월별 전비.
 */
data class MonthlyEfficiency(
    val months: List<MonthBar>,
    val avg90Days: Float,                   // 90일 평균 (점선용)
    val selectedMonth: String?,             // 선택된 월 라벨 (예: "10월")
    val dailyChart: DailyEfficiencyChart?   // 선택 시 일별 차트
)

/**
 * 월별 막대.
 */
data class MonthBar(
    val label: String,          // "5월", "6월", ...
    val avgEfficiency: Float    // 6.2, 6.4, ...
)

/**
 * 일별 전비 차트 (월 선택 시 표시).
 */
data class DailyEfficiencyChart(
    val title: String,          // "10월 일별 전비"
    val subtitle: String,       // "월 평균 5.6 · 최고 15일 6.8 · 최저 5.0"
    val dailyData: List<DailyEfficiency>,
    val avg90Days: Float        // 점선용
)

/**
 * 일별 전비 데이터.
 */
data class DailyEfficiency(
    val day: Int,               // 1~31
    val efficiency: Float       // 전비 값
)

// ===== 이번 달 예측 =====

/**
 * 이번 달 예측.
 */
data class MonthPrediction(
    val monthLabel: String,         // "11월"
    val normalRangeKm: Int,         // 380
    val predictedRangeKm: Int,      // 323
    val changePercent: Int,         // -15
    val tip: String                 // "히터 사용이 늘어..."
)
