package com.tracers.trace.ui

import com.tracers.trace.domain.MaintenanceCalculator
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceItemType
import com.tracers.trace.domain.MaintenanceItemType.*
import com.tracers.trace.ui.screens.home.MaintenanceTileMapper
import com.tracers.trace.ui.screens.home.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MaintenanceTileMapperTest {

    private val today = LocalDate.of(2026, 11, 9)

    private fun top(type: MaintenanceItemType, date: LocalDate, km: Int, odo: Double = 31_000.0) =
        MaintenanceCalculator.calculate(
            listOf(MaintenanceEntry(date = date, odometerKm = km, place = "", items = setOf(type))), null, today, odo,
        ).first()

    @Test
    fun `항목이 없으면 데이터를 모으는 중`() {
        assertEquals(MaintenanceTileMapper.Empty, MaintenanceTileMapper.map(null))
    }

    @Test
    fun `기간 항목은 남은 일과 다음 점검 날짜`() {
        val t = MaintenanceTileMapper.map(top(BATTERY_12V, LocalDate.of(2026, 1, 1), 0))
        assertEquals("1,149", t.value)
        assertEquals("일", t.unit)
        assertEquals("점검까지 여유로워요", t.status)
        assertEquals(StatusLevel.Good, t.statusLevel)
        assertEquals("12V 배터리 · 2030.01.01", t.subText)
    }

    @Test
    fun `거리 항목은 남은 km와 다음 점검 주행거리`() {
        val t = MaintenanceTileMapper.map(top(CABIN_FILTER, today, 22_200)) // 88%
        assertEquals("1,200", t.value)
        assertEquals("km", t.unit)
        assertEquals("점검이 다가와요", t.status)
        assertEquals(StatusLevel.Caution, t.statusLevel)
        assertEquals(88f, t.barPercent, 0.01f)
        assertEquals("에어컨 필터 · 32,200 km", t.subText)
    }

    @Test
    fun `90% 이상은 곧 점검, 지나면 지남`() {
        assertEquals("곧 점검해야 해요", MaintenanceTileMapper.map(top(CABIN_FILTER, today, 21_500)).status)
        val over = MaintenanceTileMapper.map(top(CABIN_FILTER, today, 20_700))
        assertEquals("300", over.value)
        assertEquals("km 지남", over.unit)
        assertEquals("점검 시기가 지났어요", over.status)
        assertEquals(StatusLevel.Warn, over.statusLevel)
        assertEquals(100f, over.barPercent, 0f)
    }
}
