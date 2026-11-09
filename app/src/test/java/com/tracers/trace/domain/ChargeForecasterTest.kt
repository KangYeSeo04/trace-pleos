package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ChargeForecasterTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val f = ChargeForecaster(zone)

    /** 2026-11-09(월) 08:00 KST */
    private val now = ms(LocalDate.of(2026, 11, 9), 8)

    private fun ms(date: LocalDate, hour: Int) =
        ZonedDateTime.of(date.atTime(hour, 0), zone).toInstant().toEpochMilli()

    private fun trip(date: LocalDate, km: Double) = TripRecord(
        startedAtMs = ms(date, 9), endedAtMs = ms(date, 10), distanceKm = km,
        energyUsedKwh = km / 5.0, hardAccelCount = 0, regenKwh = 0.0, hvacOnSec = 0,
        startBatteryPercent = null, endBatteryPercent = null,
    )

    /** 오늘 이전 [days]일 동안 평일 40km, 주말 10km */
    private fun history(days: Int): List<TripRecord> {
        val today = LocalDate.of(2026, 11, 9)
        return (1..days).map { i ->
            val d = today.minusDays(i.toLong())
            val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
            trip(d, if (weekend) 10.0 else 40.0)
        }
    }

    @Test
    fun `기록이 7일 미만이면 데이터 부족`() {
        val r = f.forecast(80, 50.0, 5.0, history(6), now)
        assertTrue(r is ChargeForecast.InsufficientData)
    }

    @Test
    fun `용량이나 전비를 모르면 계산 불가`() {
        assertTrue(f.forecast(80, null, 5.0, history(14), now) is ChargeForecast.Unavailable)
        assertTrue(f.forecast(80, 50.0, null, history(14), now) is ChargeForecast.Unavailable)
        assertTrue(f.forecast(null, 50.0, 5.0, history(14), now) is ChargeForecast.Unavailable)
    }

    @Test
    fun `요일별 평균 주행량 곱하기 전비로 5일치 예상 %, 오늘부터`() {
        // 용량 50kWh, 전비 5km/kWh → 1% = 0.5kWh = 2.5km. 평일 40km = 16%, 주말 10km = 4%
        val r = f.forecast(80, 50.0, 5.0, history(14), now) as ChargeForecast.Ready
        assertEquals(5, r.days.size)
        assertEquals(LocalDate.of(2026, 11, 9), r.days[0].date)
        assertTrue(r.days[0].isToday)
        // 월 64, 화 48, 수 32, 목 16, 금 0
        assertEquals(listOf(64, 48, 32, 16, 0), r.days.map { it.percent })
        assertEquals(listOf(false, false, false, true, true), r.days.map { it.isLow })
        assertEquals(LocalDate.of(2026, 11, 12), r.firstLowDate)
    }

    @Test
    fun `오늘 이미 달린 만큼은 오늘 예상에서 뺀다`() {
        val today = LocalDate.of(2026, 11, 9)
        val trips = history(14) + trip(today, 30.0) // 오늘 30km 달림, 평균 40km → 10km 남음
        val afternoon = ms(today, 15)
        val r = f.forecast(68, 50.0, 5.0, trips, afternoon) as ChargeForecast.Ready
        assertEquals(64, r.days[0].percent) // 68 - 10/2.5
    }

    @Test
    fun `운전하지 않은 날도 0km로 평균에 넣는다`() {
        // 14일 중 월요일 기록만 있음(11/2) + 첫 기록이 10/26(월) → 월요일 2번, 각 40km
        val trips = listOf(trip(LocalDate.of(2026, 10, 26), 40.0), trip(LocalDate.of(2026, 11, 2), 40.0))
        val r = f.forecast(80, 50.0, 5.0, trips, now) as ChargeForecast.Ready
        // 월 16% 소비, 화~금 0
        assertEquals(listOf(64, 64, 64, 64, 64), r.days.map { it.percent })
    }

    @Test
    fun `20% 미만이 없으면 firstLowDate는 null`() {
        val r = f.forecast(100, 500.0, 5.0, history(14), now) as ChargeForecast.Ready
        assertEquals(null, r.firstLowDate)
    }
}
