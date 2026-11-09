package com.tracers.trace.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class DayForecast(
    val date: LocalDate,
    /** 그날 끝의 예상 배터리 % (0~100) */
    val percent: Int,
    val isToday: Boolean,
    /** 20% 미만 → 주의색 */
    val isLow: Boolean,
)

sealed interface ChargeForecast {
    /** 기록이 7일 미만 → "데이터를 모으는 중이에요" */
    data object InsufficientData : ChargeForecast

    /** 현재 %, 용량, 전비 중 하나를 모름 */
    data object Unavailable : ChargeForecast

    data class Ready(val days: List<DayForecast>, val firstLowDate: LocalDate?) : ChargeForecast
}

/**
 * 충전 타이밍 예측 (C-02).
 *
 * 예상 % = 현재 % − 누적(요일별 평균 주행량 ÷ 전비 ÷ 용량 × 100).
 * 오늘은 평균에서 이미 달린 거리를 뺀 나머지만 쓴다.
 * 요일 평균은 첫 기록일(최대 90일 전)부터 어제까지, 운전하지 않은 날도 0km로 넣는다.
 */
class ChargeForecaster(private val zone: ZoneId) {

    fun forecast(
        currentPercent: Int?,
        capacityKwh: Double?,
        kmPerKwh: Double?,
        trips: List<TripRecord>,
        nowMs: Long,
        days: Int = FORECAST_DAYS,
    ): ChargeForecast {
        val today = dateOf(nowMs)
        val firstDate = trips.minOfOrNull { dateOf(it.startedAtMs) }
            ?: return ChargeForecast.InsufficientData
        val fullDays = ChronoUnit.DAYS.between(firstDate, today)
        if (fullDays < MIN_DATA_DAYS) return ChargeForecast.InsufficientData

        if (currentPercent == null || capacityKwh == null || capacityKwh <= 0 ||
            kmPerKwh == null || kmPerKwh <= 0
        ) return ChargeForecast.Unavailable

        val from = maxOf(firstDate, today.minusDays(WINDOW_DAYS.toLong()))
        val yesterday = today.minusDays(1)
        val avgKm = weekdayAverageKm(trips, from, yesterday)
        val drivenToday = trips.filter { dateOf(it.startedAtMs) == today }.sumOf { it.distanceKm }

        var percent = currentPercent.toDouble()
        val result = (0 until days).map { i ->
            val date = today.plusDays(i.toLong())
            val plannedKm = avgKm[date.dayOfWeek] ?: 0.0
            val km = if (i == 0) (plannedKm - drivenToday).coerceAtLeast(0.0) else plannedKm
            percent -= km / kmPerKwh / capacityKwh * 100.0
            val p = percent.roundToInt().coerceIn(0, 100)
            DayForecast(date = date, percent = p, isToday = i == 0, isLow = p < LOW_PERCENT)
        }
        return ChargeForecast.Ready(result, result.firstOrNull { it.isLow }?.date)
    }

    private fun weekdayAverageKm(
        trips: List<TripRecord>,
        from: LocalDate,
        to: LocalDate,
    ): Map<DayOfWeek, Double> {
        val occurrences = mutableMapOf<DayOfWeek, Int>()
        var d = from
        while (!d.isAfter(to)) {
            occurrences.merge(d.dayOfWeek, 1, Int::plus)
            d = d.plusDays(1)
        }
        val kmByDow = trips
            .filter { val t = dateOf(it.startedAtMs); !t.isBefore(from) && !t.isAfter(to) }
            .groupBy { dateOf(it.startedAtMs).dayOfWeek }
            .mapValues { (_, list) -> list.sumOf { it.distanceKm } }
        return occurrences.mapValues { (dow, n) -> (kmByDow[dow] ?: 0.0) / n }
    }

    private fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    companion object {
        const val FORECAST_DAYS = 5
        const val MIN_DATA_DAYS = 7L
        const val LOW_PERCENT = 20
        private const val WINDOW_DAYS = 90
    }
}
