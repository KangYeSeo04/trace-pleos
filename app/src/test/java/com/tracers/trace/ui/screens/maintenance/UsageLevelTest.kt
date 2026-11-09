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
    fun `양식은 항목을 하나 이상 골라야 저장 가능`() {
        assertEquals(false, MaintenanceRecordForm().canSave)
        assertEquals(true, MaintenanceRecordForm(selectedItems = setOf("와이퍼")).canSave)
    }
}
