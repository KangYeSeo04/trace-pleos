package com.tracers.trace.ui.screens.maintenance

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageLevelTest {

    @Test
    fun `90 이상 경고, 75 이상 주의, 그 외 회색`() {
        assertEquals(UsageLevel.Warn, UsageLevel.of(90f))
        assertEquals(UsageLevel.Caution, UsageLevel.of(89.9f))
        assertEquals(UsageLevel.Caution, UsageLevel.of(75f))
        assertEquals(UsageLevel.Normal, UsageLevel.of(74.9f))
    }

    @Test
    fun `항목 단계는 가장 많이 쓴 막대 기준`() {
        val item = MaintenanceItem(
            name = "에어컨 필터",
            remaining = "1,200 km 남음",
            gauges = listOf(UsageGauge(60f, "6 / 10"), UsageGauge(92f, "11 / 12개월")),
        )
        assertEquals(UsageLevel.Warn, item.level)
    }

    @Test
    fun `양식은 항목을 하나 이상 고르고 날짜·주행거리를 읽을 수 있어야 저장 가능`() {
        val filled = MaintenanceRecordForm(dateText = "2026. 11. 9", odometerDigits = "38800")
        assertEquals(false, filled.canSave)
        assertEquals(true, filled.copy(selectedItems = setOf("와이퍼")).canSave)
        assertEquals(false, filled.copy(selectedItems = setOf("와이퍼"), odometerDigits = "").canSave)
        assertEquals(false, filled.copy(selectedItems = setOf("와이퍼"), dateText = "11월 9일").canSave)
    }
}
