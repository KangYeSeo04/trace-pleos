package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class EfficiencyCalculatorTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val calc = EfficiencyCalculator(zone)

    /** 2026-11-09 18:00 KST */
    private val now = ZonedDateTime.of(2026, 11, 9, 18, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val dayMs = 86_400_000L

    private fun trip(
        startMs: Long, km: Double, used: Double, regen: Double = 0.0,
        durSec: Long = 1800, hvacSec: Long = 0, accel: Int = 0,
    ) = TripRecord(
        startedAtMs = startMs, endedAtMs = startMs + durSec * 1000, distanceKm = km,
        energyUsedKwh = used, hardAccelCount = accel, regenKwh = regen, hvacOnSec = hvacSec,
        startBatteryPercent = null, endBatteryPercent = null,
    )

    @Test
    fun `주행 전비 = 거리 나누기 (소비 - 회생)`() {
        assertEquals(6.0, calc.tripEfficiency(trip(now, km = 12.0, used = 2.5, regen = 0.5))!!, 1e-6)
    }

    @Test
    fun `쓴 에너지가 0 이하면 전비를 내지 않는다`() {
        assertNull(calc.tripEfficiency(trip(now, km = 3.0, used = 0.2, regen = 0.2)))
        assertNull(calc.tripEfficiency(trip(now, km = 3.0, used = 0.0)))
    }

    @Test
    fun `일별 전비는 그날 거리 합 나누기 에너지 합 (거리 가중)`() {
        val d1 = now - 2 * dayMs
        val trips = listOf(
            trip(d1, km = 10.0, used = 2.0),        // 5.0
            trip(d1 + 3_600_000, km = 30.0, used = 4.0), // 7.5
            trip(now - 3_600_000, km = 6.0, used = 1.0),
        )
        val daily = calc.daily(trips)
        assertEquals(2, daily.size)
        assertEquals(LocalDate.of(2026, 11, 7), daily[0].date)
        assertEquals(40.0 / 6.0, daily[0].kmPerKwh!!, 1e-6)
        assertEquals(40.0, daily[0].distanceKm, 1e-6)
        assertEquals(LocalDate.of(2026, 11, 9), daily[1].date)
    }

    @Test
    fun `날짜는 현지 시간대 기준으로 나눈다`() {
        // 2026-11-08 23:30 KST = 14:30 UTC
        val lateNight = ZonedDateTime.of(2026, 11, 8, 23, 30, 0, 0, zone).toInstant().toEpochMilli()
        val daily = calc.daily(listOf(trip(lateNight, km = 5.0, used = 1.0)))
        assertEquals(LocalDate.of(2026, 11, 8), daily.single().date)
    }

    @Test
    fun `90일 평균은 90일 안의 주행만, 오늘과 평균 차이를 낸다`() {
        val trips = listOf(
            trip(now - 120 * dayMs, km = 100.0, used = 100.0), // 90일 밖: 제외
            trip(now - 10 * dayMs, km = 50.0, used = 10.0),
            trip(now - 5 * dayMs, km = 30.0, used = 6.0),
            trip(now - 3_600_000, km = 12.0, used = 2.0),     // 오늘 6.0
        )
        val s = calc.summarize(trips, emptyList(), now)
        assertEquals(92.0 / 18.0, s.averageKmPerKwh!!, 1e-6)
        assertEquals(6.0, s.todayKmPerKwh!!, 1e-6)
        assertEquals(6.0 - 92.0 / 18.0, s.todayDiffKmPerKwh!!, 1e-6)
        assertEquals(true, s.isTodayAboveAverage)
        assertEquals(92.0, s.totalDistanceKm, 1e-6)
    }

    @Test
    fun `요약 4칸 - 회생 kWh, 공조 비중, 급가속, 충전 횟수`() {
        val trips = listOf(
            trip(now - 2 * dayMs, km = 20.0, used = 4.0, regen = 0.6, durSec = 3000, hvacSec = 1500, accel = 2),
            trip(now - 1 * dayMs, km = 10.0, used = 2.0, regen = 0.4, durSec = 1000, hvacSec = 0, accel = 1),
        )
        val charges = listOf(
            ChargeRecord(now - 100 * dayMs, now - 100 * dayMs, 20, 80, 46.0), // 90일 밖
            ChargeRecord(now - 3 * dayMs, now - 3 * dayMs, 30, 80, 38.0),
            ChargeRecord(now - dayMs, now - dayMs, 50, 90, 31.0),
        )
        val s = calc.summarize(trips, charges, now)
        assertEquals(1.0, s.regenKwh, 1e-6)
        assertEquals(38, s.hvacSharePercent) // 1500 / 4000 = 37.5 → 38
        assertEquals(3, s.hardAccelCount)
        assertEquals(2, s.chargeCount)
    }

    @Test
    fun `오늘 주행이 없으면 오늘 값과 비교는 null`() {
        val s = calc.summarize(listOf(trip(now - 3 * dayMs, km = 10.0, used = 2.0)), emptyList(), now)
        assertEquals(5.0, s.averageKmPerKwh!!, 1e-6)
        assertNull(s.todayKmPerKwh)
        assertNull(s.todayDiffKmPerKwh)
        assertNull(s.isTodayAboveAverage)
    }

    @Test
    fun `기록이 없으면 모두 비어 있다`() {
        val s = calc.summarize(emptyList(), emptyList(), now)
        assertNull(s.averageKmPerKwh)
        assertNull(s.hvacSharePercent)
        assertEquals(0, s.chargeCount)
        assertEquals(0, s.daily.size)
    }
}
