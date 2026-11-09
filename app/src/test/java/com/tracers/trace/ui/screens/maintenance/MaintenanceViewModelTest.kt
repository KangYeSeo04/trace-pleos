package com.tracers.trace.ui.screens.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceViewModelTest {

    private val today = LocalDate.of(2026, 11, 9)

    @Test
    fun `빈 상태로 시작하면 날짜는 오늘, 달력은 이번 달`() {
        val vm = MaintenanceViewModel(today = today)
        val s = vm.uiState.value
        assertEquals("2026. 11. 9", s.form.dateText)
        assertEquals(11, s.calendar?.month?.monthValue)
    }

    @Test
    fun `주행거리·비용은 숫자만, 메모는 200자까지`() {
        val vm = MaintenanceViewModel(today = today)
        vm.onOdometerChange("38,800 km")
        vm.onCostChange("0080000원")
        vm.onMemoChange("가".repeat(250))
        val f = vm.uiState.value.form
        assertEquals("38800", f.odometerDigits)
        assertEquals("80000", f.costDigits)
        assertEquals(200, f.memo.length)
    }

    @Test
    fun `항목 칩은 여러 개 선택, 다시 누르면 해제, 1개 이상이면 저장 가능`() {
        val vm = MaintenanceViewModel(today = today)
        assertFalse(vm.uiState.value.form.canSave)
        vm.onToggleItem("와이퍼")
        vm.onToggleItem("기타")
        assertEquals(setOf("와이퍼", "기타"), vm.uiState.value.form.selectedItems)
        vm.onToggleItem("와이퍼")
        vm.onToggleItem("기타")
        assertFalse(vm.uiState.value.form.canSave)
        vm.onToggleItem("기타")
        assertTrue(vm.uiState.value.form.canSave)
    }
}
