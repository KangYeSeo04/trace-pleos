package com.tracers.trace.ui

import com.tracers.trace.data.record.EfficiencySnapshot
import com.tracers.trace.domain.ChargeForecast
import com.tracers.trace.domain.DayForecast
import com.tracers.trace.domain.EfficiencySummary
import com.tracers.trace.domain.MaintenanceCalculator
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceItemType
import com.tracers.trace.domain.TripRecord
import com.tracers.trace.ui.screens.home.ParkedCaptionMapper
import com.tracers.trace.ui.screens.home.ParkedCaptionMapper.Caption
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ParkedCaptionMapperTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 11, 9) // 월
    private val now = ZonedDateTime.of(today.atTime(18, 0), zone).toInstant().toEpochMilli()

    /** 10일 전 첫 주행, 90일 거리 340 km → 하루 평균 34 km */
    private val trips = listOf(
        TripRecord(now - 10 * 86_400_000L, now - 10 * 86_400_000L + 600_000, 340.0, 50.0, 0, 0.0, 0, 90, 30),
    )
    private val summary = EfficiencySummary(
        averageKmPerKwh = 6.0, todayKmPerKwh = null, todayDiffKmPerKwh = null, isTodayAboveAverage = null,
        totalDistanceKm = 340.0, regenKwh = 0.0, hvacSharePercent = null, hardAccelCount = 0, chargeCount = 0, daily = emptyList(),
    )

    private fun days(vararg pct: Int) = pct.mapIndexed { i, p ->
        DayForecast(today.plusDays(i.toLong()), p, i == 0, p < 20)
    }

    private fun snap(forecast: ChargeForecast, percent: Int? = 64) =
        EfficiencySnapshot(summary, forecast, trips, emptyList(), percent, now, zone)

    @Test
    fun `충전 중이 가장 먼저`() {
        val r = ParkedCaptionMapper.map(charging = true, snap(ChargeForecast.Ready(days(64, 55, 45, 35, 25), null)))
        assertEquals(Caption("충전 중", "배터리 64% · 하루 평균 34 km", bolt = true), r)
    }

    @Test
    fun `5일 안에 20% 미만이 안 되면 마지막 날까지 여유`() {
        val r = ParkedCaptionMapper.map(false, snap(ChargeForecast.Ready(days(64, 55, 45, 35, 25), null)))
        assertEquals(Caption("충전은 금요일까지 여유 있어요", "배터리 64% · 하루 평균 34 km 기준 · 추정", bolt = true), r)
    }

    @Test
    fun `5일 안에 20% 미만이 되면 그 요일 전에 충전`() {
        val d = days(40, 30, 21, 12, 3)
        val r = ParkedCaptionMapper.map(false, snap(ChargeForecast.Ready(d, d[3].date)))
        assertEquals("목요일 전에 충전하면 좋아요", r.title)
        val t = ParkedCaptionMapper.map(false, snap(ChargeForecast.Ready(days(15, 5, 0, 0, 0), today)))
        assertEquals("오늘 충전하면 좋아요", t.title)
    }

    @Test
    fun `예측이 없으면 쉬는 중, 보조 줄은 아는 값만`() {
        assertEquals(Caption("쉬는 중", "배터리 64% · 하루 평균 34 km"), ParkedCaptionMapper.map(false, snap(ChargeForecast.InsufficientData)))
        assertEquals(Caption("쉬는 중", "하루 평균 34 km"), ParkedCaptionMapper.map(false, snap(ChargeForecast.Unavailable, percent = null)))
        assertEquals(Caption("쉬는 중", ""), ParkedCaptionMapper.map(false, null))
    }

    private val wiperDue10Days = MaintenanceCalculator.calculate(
        listOf(MaintenanceEntry(date = today.minusMonths(12).plusDays(10), odometerKm = 30_000, place = "", items = setOf(MaintenanceItemType.WIPER))),
        null, today, 31_000.0,
    ).single()

    @Test
    fun `점검 임박은 충전 중 다음, 충전 타이밍보다 먼저`() {
        val ready = snap(ChargeForecast.Ready(days(40, 30, 21, 12, 3), today.plusDays(3)))
        assertEquals("충전 중", ParkedCaptionMapper.map(true, ready, wiperDue10Days).title)
        assertEquals(
            Caption("점검이 다가와요", "와이퍼 블레이드 · 10일 남음"),
            ParkedCaptionMapper.map(false, ready, wiperDue10Days),
        )
        assertEquals("목요일 전에 충전하면 좋아요", ParkedCaptionMapper.map(false, ready, null).title)
    }
}
