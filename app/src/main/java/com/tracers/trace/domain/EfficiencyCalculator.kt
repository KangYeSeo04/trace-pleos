package com.tracers.trace.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** 하루 전비 */
data class DailyEfficiency(
    val date: LocalDate,
    val distanceKm: Double,
    /** 쓴 에너지가 0 이하면 null */
    val kmPerKwh: Double?,
)

/**
 * 전비 요약 (C-01). 모든 전비는 앱이 잔량 감소량 ÷ 거리로 계산한 **추정** 값이다.
 */
data class EfficiencySummary(
    /** 90일 평균 (거리 가중) */
    val averageKmPerKwh: Double?,
    val todayKmPerKwh: Double?,
    /** 오늘 − 평균 */
    val todayDiffKmPerKwh: Double?,
    val isTodayAboveAverage: Boolean?,
    val totalDistanceKm: Double,
    /** 90일 회생제동 추정 kWh */
    val regenKwh: Double,
    /** 90일 주행 시간 중 공조 켠 비중 (0~100). 공조 신호 미지원이면 null */
    val hvacSharePercent: Int?,
    val hardAccelCount: Int,
    val chargeCount: Int,
    /** 90일 일별 전비, 날짜 오름차순 */
    val daily: List<DailyEfficiency>,
)

/**
 * 주행별·일별·90일 평균 전비와 오늘 비교를 계산한다 (C-01). 순수 계산, 상태 없음.
 */
class EfficiencyCalculator(private val zone: ZoneId) {

    /** 주행 1회 전비 (km/kWh) */
    fun tripEfficiency(trip: TripRecord): Double? = efficiency(trip.distanceKm, trip.netEnergyKwh)

    fun daily(trips: List<TripRecord>): List<DailyEfficiency> =
        trips.groupBy { dateOf(it.startedAtMs) }
            .toSortedMap()
            .map { (date, list) ->
                val km = list.sumOf { it.distanceKm }
                DailyEfficiency(date, km, efficiency(km, list.sumOf { it.netEnergyKwh }))
            }

    fun summarize(
        trips: List<TripRecord>,
        charges: List<ChargeRecord>,
        nowMs: Long,
        windowDays: Int = WINDOW_DAYS,
        /** 공조 전원 신호를 지원하지 않는 차면 false → 공조 비중 null ("—") */
        hvacSupported: Boolean = true,
    ): EfficiencySummary {
        val today = dateOf(nowMs)
        val from = today.minusDays(windowDays.toLong() - 1)
        val inWindow = trips.filter { !dateOf(it.startedAtMs).isBefore(from) && it.startedAtMs <= nowMs }

        val km = inWindow.sumOf { it.distanceKm }
        val average = efficiency(km, inWindow.sumOf { it.netEnergyKwh })

        val todayTrips = inWindow.filter { dateOf(it.startedAtMs) == today }
        val todayValue = if (todayTrips.isEmpty()) null
        else efficiency(todayTrips.sumOf { it.distanceKm }, todayTrips.sumOf { it.netEnergyKwh })
        val diff = if (todayValue != null && average != null) todayValue - average else null

        val durSec = inWindow.sumOf { it.durationSec }
        val hvacShare = if (hvacSupported && durSec > 0) (inWindow.sumOf { it.hvacOnSec } * 100.0 / durSec).roundToInt().coerceAtMost(100) else null

        return EfficiencySummary(
            averageKmPerKwh = average,
            todayKmPerKwh = todayValue,
            todayDiffKmPerKwh = diff,
            isTodayAboveAverage = diff?.let { it >= 0 },
            totalDistanceKm = km,
            regenKwh = inWindow.sumOf { it.regenKwh },
            hvacSharePercent = hvacShare,
            hardAccelCount = inWindow.sumOf { it.hardAccelCount },
            chargeCount = charges.count { !dateOf(it.startedAtMs).isBefore(from) && it.startedAtMs <= nowMs },
            daily = daily(inWindow),
        )
    }

    private fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    private fun efficiency(km: Double, netKwh: Double): Double? =
        if (netKwh > MIN_NET_KWH && km > 0) km / netKwh else null

    companion object {
        const val WINDOW_DAYS = 90
        /** 이보다 적게 쓴 경우 나눗셈이 튀므로 전비를 내지 않는다 */
        private const val MIN_NET_KWH = 0.01
    }
}
