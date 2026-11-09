package com.tracers.trace.ui

import com.tracers.trace.domain.ChargeForecast
import com.tracers.trace.domain.ChargeRecord
import com.tracers.trace.domain.DayForecast
import com.tracers.trace.domain.EfficiencySummary
import com.tracers.trace.domain.TripRecord
import com.tracers.trace.ui.screens.efficiency.EfficiencyUiMapper
import com.tracers.trace.ui.screens.home.EfficiencyTileMapper
import com.tracers.trace.ui.screens.home.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class EfficiencyMappersTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val today = LocalDate.of(2026, 11, 9) // 월
    private val now = ZonedDateTime.of(today.atTime(18, 0), zone).toInstant().toEpochMilli()

    private fun summary(avg: Double? = 5.0, todayV: Double? = 6.0) = EfficiencySummary(
        averageKmPerKwh = avg, todayKmPerKwh = todayV,
        todayDiffKmPerKwh = if (avg != null && todayV != null) todayV - avg else null,
        isTodayAboveAverage = if (avg != null && todayV != null) todayV >= avg else null,
        totalDistanceKm = 120.0, regenKwh = 0.84, hvacSharePercent = 17, hardAccelCount = 2,
        chargeCount = 1, daily = emptyList(),
    )

    private fun trip(daysAgo: Long, km: Double, kwh: Double): TripRecord {
        val t = ZonedDateTime.of(today.minusDays(daysAgo).atTime(9, 0), zone).toInstant().toEpochMilli()
        return TripRecord(t, t + 1_800_000, km, kwh, 0, 0.0, 0, null, null)
    }

    // ---- 홈 타일 ----

    @Test
    fun `기록이 없으면 타일은 지금의 빈 상태`() {
        assertEquals(EfficiencyTileMapper.Empty, EfficiencyTileMapper.map(null, false))
        assertEquals(EfficiencyTileMapper.Empty, EfficiencyTileMapper.map(summary(avg = null, todayV = null), false))
    }

    @Test
    fun `정차 타일 - 오늘 값, 평균과 차이, 보조 줄에 추정`() {
        val t = EfficiencyTileMapper.map(summary(), isDriving = false)
        assertEquals("6.0", t.value)
        assertEquals("평균보다 1.0 좋아요", t.status)
        assertEquals(StatusLevel.Good, t.statusLevel)
        assertEquals("추정 · 공조 17% · 회생 0.8 kWh", t.subText)
        assertEquals(60f, t.barPercent, 1e-3f)
    }

    @Test
    fun `평균보다 낮으면 주의색, 주행 중에는 상태 줄에 추정`() {
        val t = EfficiencyTileMapper.map(summary(todayV = 4.0), isDriving = true)
        assertEquals("추정 · 평소보다 낮아요", t.status)
        assertEquals(StatusLevel.Caution, t.statusLevel)
    }

    @Test
    fun `오늘 주행이 없으면 90일 평균을 보여 준다`() {
        val t = EfficiencyTileMapper.map(summary(todayV = null), isDriving = false)
        assertEquals("5.0", t.value)
        assertEquals(StatusLevel.Normal, t.statusLevel)
    }

    // ---- 전비 화면 ----

    @Test
    fun `기록이 없으면 섹션은 모두 null (데이터를 모으는 중)`() {
        val ui = EfficiencyUiMapper.map(
            summary(avg = null, todayV = null), ChargeForecast.InsufficientData,
            emptyList(), emptyList(), 80, now, zone,
        )
        assertNull(ui.average90Days)
        assertNull(ui.chargeTiming)
        assertNull(ui.calendar)
        assertNull(ui.monthlyEfficiency)
    }

    @Test
    fun `90일 평균은 추정으로 표시하고 오늘 비교를 붙인다`() {
        val ui = EfficiencyUiMapper.map(
            summary(), ChargeForecast.InsufficientData, listOf(trip(0, 12.0, 2.0)), emptyList(), 80, now, zone,
        )
        val avg = ui.average90Days!!
        assertTrue(avg.isEstimated)
        assertEquals(1.0f, avg.todayComparison!!.difference, 1e-4f)
        assertTrue(avg.todayComparison!!.isAboveAverage)
        assertNull(ui.chargeTiming) // 7일 미만
    }

    @Test
    fun `충전 타이밍 - 지금, 내일, 요일 라벨과 문구`() {
        val days = (0..4).map { i ->
            val p = listOf(64, 48, 32, 16, 0)[i]
            DayForecast(today.plusDays(i.toLong()), p, i == 0, p < 20)
        }
        val ready = ChargeForecast.Ready(days, today.plusDays(3))
        val ct = EfficiencyUiMapper.map(summary(), ready, listOf(trip(10, 120.0, 24.0)), emptyList(), 64, now, zone)
            .chargeTiming!!
        assertEquals(listOf("지금", "내일", "수", "목", "금"), ct.forecast.map { it.label })
        assertEquals("목요일 전에 충전하세요", ct.title)
        assertEquals("배터리 64% · 하루 평균 12 km 기준 · 추정", ct.description)
    }

    @Test
    fun `달력은 이번 달에 주행·충전한 날에 점, 미래는 비활성`() {
        val chargeAt = ZonedDateTime.of(today.minusDays(2).atTime(20, 0), zone).toInstant().toEpochMilli()
        val cal = EfficiencyUiMapper.calendar(
            listOf(trip(0, 10.0, 2.0), trip(1, 10.0, 2.0)),
            listOf(ChargeRecord(chargeAt, chargeAt + 1, 30, 80, 38.0)),
            today, zone,
        )
        assertEquals(30, cal.days.size)
        assertTrue(cal.days[8].hasDriving && cal.days[8].isToday)
        assertTrue(cal.days[7].hasDriving)
        assertTrue(cal.days[6].hasCharging)
        assertTrue(cal.days[9].isFuture)
    }

    @Test
    fun `월별 막대는 달마다 거리 합 나누기 에너지 합`() {
        val m = EfficiencyUiMapper.monthly(
            summary(),
            listOf(trip(20, 50.0, 10.0), trip(0, 30.0, 5.0), trip(1, 30.0, 5.0)), // 10/20, 11/9, 11/8
            zone,
        )
        assertNotNull(m)
        assertEquals(listOf("10월", "11월"), m!!.months.map { it.label })
        assertEquals(5.0f, m.months[0].avgEfficiency, 1e-4f)
        assertEquals(6.0f, m.months[1].avgEfficiency, 1e-4f)
    }
}
