package com.tracers.trace.ui.screens.maintenance

import com.tracers.trace.domain.MaintenanceBaseline
import com.tracers.trace.domain.MaintenanceCalculator
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceInputMethod
import com.tracers.trace.domain.MaintenanceItemType
import com.tracers.trace.domain.MaintenanceItemType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MaintenanceUiMapperTest {

    private val today = LocalDate.of(2026, 11, 9)

    private fun status(type: MaintenanceItemType, date: LocalDate, km: Int, odo: Double? = 31_000.0) =
        MaintenanceCalculator.calculate(
            listOf(MaintenanceEntry(date = date, odometerKm = km, place = "", items = setOf(type))), null, today, odo,
        ).single()

    @Test
    fun `거리 항목은 남은 km와 사용 대 주기`() {
        val item = MaintenanceUiMapper.item(status(CABIN_FILTER, LocalDate.of(2026, 1, 8), 22_200))
        assertEquals("1,200 km 남음", item.remaining)
        assertEquals("8,800 / 10,000 km", item.gauges.single().basis)
        assertEquals(88f, item.gauges.single().percent, 0.01f)
        assertNull(item.basisNote)
    }

    @Test
    fun `기간 항목은 남은 일·개월·년과 지난 개월 대 주기`() {
        val wiper = MaintenanceUiMapper.item(status(WIPER, LocalDate.of(2025, 11, 20), 0))
        assertEquals("11일 남음", wiper.remaining)
        assertEquals("11.6 / 12개월", wiper.gauges.single().basis)

        assertEquals("7개월 남음", MaintenanceUiMapper.days(213))
        assertEquals("약 2년 남음", MaintenanceUiMapper.days(730))
        assertEquals("60일 남음", MaintenanceUiMapper.days(60))
        assertEquals("3일 지남", MaintenanceUiMapper.days(-3))
    }

    @Test
    fun `지난 거리, 모르는 주행거리`() {
        assertEquals("300 km 지남", MaintenanceUiMapper.item(status(CABIN_FILTER, today, 20_700)).remaining)
        val unknown = MaintenanceUiMapper.item(status(CABIN_FILTER, today, 20_700, odo = null))
        assertEquals("주행거리 확인 중", unknown.remaining)
        assertEquals("– / 10,000 km", unknown.gauges.single().basis)
    }

    @Test
    fun `기록이 없어 처음 본 값 기준이면 기준 표시`() {
        val s = MaintenanceCalculator.calculate(emptyList(), MaintenanceBaseline(today, 30_000.0), today, 30_000.0)
        assertEquals(MaintenanceUiMapper.FIRST_SEEN_NOTE, MaintenanceUiMapper.item(s.first()).basisNote)
    }

    @Test
    fun `기록 제목은 항목 이름 순서대로, 기타는 맨 뒤`() {
        val r = MaintenanceUiMapper.record(
            MaintenanceEntry(
                id = 7, date = today, odometerKm = 38_800, place = "정비소",
                items = setOf(BRAKE_FLUID, TIRE_ROTATION), other = true, costWon = 80_000,
                method = MaintenanceInputMethod.AUTO_DETECTED,
            )
        )
        assertEquals("타이어 위치 교환 · 브레이크액 · 기타", r.title)
        assertEquals(listOf("타이어 위치 교환", "브레이크액"), r.itemNames)
        assertEquals(RecordInputMethod.AutoDetected, r.method)
    }

    @Test
    fun `항목 칩은 8개 항목과 기타`() {
        assertEquals(9, MaintenanceUiMapper.CHIP_OPTIONS.size)
        assertEquals("기타", MaintenanceUiMapper.CHIP_OPTIONS.last())
    }
}
