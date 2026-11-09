package com.tracers.trace.ui.screens.efficiency

import com.tracers.trace.domain.ChargeForecast
import com.tracers.trace.domain.ChargeRecord
import com.tracers.trace.domain.TripRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt
import com.tracers.trace.domain.EfficiencySummary as DomainSummary

/**
 * domain 계산 결과 → 전비 화면 [EfficiencyUiState]. 순수 함수.
 *
 * 값이 없는 섹션은 null로 두어 화면이 "데이터를 모으는 중이에요"를 그린다.
 * 모든 전비·예측은 앱 계산값이라 "추정"으로 표시한다.
 */
object EfficiencyUiMapper {

    fun map(
        summary: DomainSummary,
        forecast: ChargeForecast,
        trips: List<TripRecord>,
        charges: List<ChargeRecord>,
        currentPercent: Int?,
        nowMs: Long,
        zone: ZoneId,
    ): EfficiencyUiState {
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return EfficiencyUiState(
            isLoading = false,
            average90Days = average(summary),
            chargeTiming = chargeTiming(forecast, summary, trips, currentPercent, today, zone),
            calendar = if (trips.isEmpty() && charges.isEmpty()) null else calendar(trips, charges, today, zone),
            monthlyEfficiency = monthly(summary, trips, zone),
            // TODO(C-03·C-04): 이번 달 예측
            prediction = null,
        )
    }

    private fun average(s: DomainSummary): EfficiencyAverage? {
        val avg = s.averageKmPerKwh ?: return null
        val today = s.todayKmPerKwh
        val diff = s.todayDiffKmPerKwh
        return EfficiencyAverage(
            avgEfficiency = avg.toFloat(),
            todayComparison = if (today != null && diff != null) {
                TodayComparison(today.toFloat(), kotlin.math.abs(diff).toFloat(), diff >= 0)
            } else null,
            summary = EfficiencySummary(
                regenBraking = s.regenKwh.toFloat(),
                hvac = s.hvacSharePercent ?: 0,
                hardAccel = s.hardAccelCount,
                charges = s.chargeCount,
            ),
            isEstimated = true,
        )
    }

    internal fun chargeTiming(
        forecast: ChargeForecast,
        summary: DomainSummary,
        trips: List<TripRecord>,
        currentPercent: Int?,
        today: LocalDate,
        zone: ZoneId,
    ): ChargeTiming? {
        val ready = forecast as? ChargeForecast.Ready ?: return null
        val firstDate = trips.minOfOrNull { Instant.ofEpochMilli(it.startedAtMs).atZone(zone).toLocalDate() }
        val days = firstDate?.let { java.time.temporal.ChronoUnit.DAYS.between(it, today).coerceIn(1, 90) } ?: 1
        val dailyKm = (summary.totalDistanceKm / days).roundToInt()

        val low = ready.firstLowDate
        val title = when {
            low == null -> "${weekdayName(ready.days.last().date.dayOfWeek)}요일까지 충분해요"
            low == today -> "오늘 충전하는 게 좋아요"
            low == today.plusDays(1) -> "내일 충전하는 게 좋아요"
            else -> "${weekdayName(low.dayOfWeek)}요일 전에 충전하세요"
        }
        val percentText = currentPercent?.let { "배터리 $it% · " } ?: ""
        return ChargeTiming(
            title = title,
            description = "${percentText}하루 평균 $dailyKm km 기준 · 추정",
            forecast = ready.days.mapIndexed { i, d ->
                ChargeForecastDay(
                    label = when (i) {
                        0 -> "지금"
                        1 -> "내일"
                        else -> weekdayName(d.date.dayOfWeek)
                    },
                    percentage = d.percent,
                    isToday = d.isToday,
                )
            },
        )
    }

    internal fun calendar(
        trips: List<TripRecord>,
        charges: List<ChargeRecord>,
        today: LocalDate,
        zone: ZoneId,
    ): DrivingCalendar {
        val month = YearMonth.from(today)
        fun dateOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        val driveDays = trips.map { dateOf(it.startedAtMs) }.filter { YearMonth.from(it) == month }.map { it.dayOfMonth }.toSet()
        val chargeDays = charges.map { dateOf(it.startedAtMs) }.filter { YearMonth.from(it) == month }.map { it.dayOfMonth }.toSet()
        return DrivingCalendar(
            year = month.year,
            month = month.monthValue,
            days = (1..month.lengthOfMonth()).map { d ->
                CalendarDay(
                    dayOfMonth = d,
                    hasDriving = d in driveDays,
                    hasCharging = d in chargeDays,
                    isFuture = d > today.dayOfMonth,
                    isToday = d == today.dayOfMonth,
                )
            },
            // TODO(C-01): 날짜 선택 상세 — 화면에 선택 이벤트를 붙일 때
            selectedDay = null,
            selectedDayDetail = null,
        )
    }

    internal fun monthly(summary: DomainSummary, trips: List<TripRecord>, zone: ZoneId): MonthlyEfficiency? {
        val avg = summary.averageKmPerKwh ?: return null
        val months = trips
            .groupBy { YearMonth.from(Instant.ofEpochMilli(it.startedAtMs).atZone(zone)) }
            .toSortedMap()
            .mapNotNull { (ym, list) ->
                val net = list.sumOf { it.netEnergyKwh }
                if (net <= 0.0) null
                else MonthBar(label = "${ym.monthValue}월", avgEfficiency = (list.sumOf { it.distanceKm } / net).toFloat())
            }
        if (months.isEmpty()) return null
        return MonthlyEfficiency(
            months = months,
            avg90Days = avg.toFloat(),
            selectedMonth = null,
            // TODO(C-01): 월 선택 시 일별 차트 — 선택 이벤트를 ViewModel로 올릴 때
            dailyChart = null,
        )
    }

    private fun weekdayName(d: DayOfWeek) = when (d) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }
}
