package com.tracers.trace.ui.screens.maintenance

import com.tracers.trace.data.record.MaintenanceSnapshot
import com.tracers.trace.domain.MaintenanceCalculator
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceInputMethod
import com.tracers.trace.domain.MaintenanceItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceViewModelTest {

    private val today = LocalDate.of(2026, 11, 9)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

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
        vm.onOdometerChange("38800")
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

    @Test
    fun `날짜를 읽을 수 없거나 주행거리가 비면 저장할 수 없다`() {
        val vm = MaintenanceViewModel(today = today)
        vm.onToggleItem("와이퍼")
        assertFalse(vm.uiState.value.form.canSave)
        vm.onOdometerChange("100")
        assertTrue(vm.uiState.value.form.canSave)
        vm.onDateChange("어제")
        assertFalse(vm.uiState.value.form.canSave)
    }

    /** 저장소 흉내: 저장하면 기록 목록이 바뀌고 계산이 다시 돈다 */
    private class FakeStore(odo: Double?) {
        val records = MutableStateFlow<List<MaintenanceEntry>>(emptyList())
        val odometer = MutableStateFlow(odo)
        val saved = mutableListOf<MaintenanceEntry>()
        val snapshots = MutableStateFlow<MaintenanceSnapshot?>(null)

        fun emit(today: LocalDate) {
            snapshots.value = MaintenanceSnapshot(
                MaintenanceCalculator.calculate(records.value, null, today, odometer.value), records.value, odometer.value, today,
            )
        }

        suspend fun save(e: MaintenanceEntry) {
            saved += e
            records.value = listOf(e.copy(id = saved.size.toLong())) + records.value
        }
    }

    @Test
    fun `주행거리 칸 기본값은 지금 총 주행거리, 고친 뒤에는 덮어쓰지 않는다`() {
        val store = FakeStore(38_812.7).apply { emit(today) }
        val vm = MaintenanceViewModel(today = today, snapshots = store.snapshots.filterNotNull())
        assertEquals("38812", vm.uiState.value.form.odometerDigits)
        vm.onOdometerChange("38000")
        store.odometer.value = 38_900.0
        store.emit(today)
        assertEquals("38000", vm.uiState.value.form.odometerDigits)
    }

    @Test
    fun `저장하면 기록이 남고 항목 주기가 다시 계산되고 양식이 비워진다`() {
        val store = FakeStore(38_800.0).apply { emit(today) }
        val vm = MaintenanceViewModel(
            today = today,
            snapshots = store.snapshots.filterNotNull(),
            saveRecord = { store.save(it); store.emit(today) },
        )
        assertTrue(vm.uiState.value.items.isEmpty())

        vm.onToggleItem("타이어 위치 교환")
        vm.onToggleItem("기타")
        vm.onPlaceChange("  [정비소]  ")
        vm.onCostChange("80000")
        vm.onSave()

        val e = store.saved.single()
        assertEquals(today, e.date)
        assertEquals(38_800, e.odometerKm)
        assertEquals("[정비소]", e.place)
        assertEquals(setOf(MaintenanceItemType.TIRE_ROTATION), e.items)
        assertTrue(e.other)
        assertEquals(80_000, e.costWon)
        assertEquals(MaintenanceInputMethod.MANUAL, e.method)

        val s = vm.uiState.value
        assertEquals(listOf("타이어 위치 교환"), s.items.map { it.name })
        assertEquals("10,000 km 남음", s.items.single().remaining)
        assertEquals("타이어 위치 교환 · 기타", s.records.single().title)
        assertTrue(s.form.selectedItems.isEmpty())
        assertEquals("", s.form.costDigits)
        assertEquals("38800", s.form.odometerDigits)
        assertEquals("2026. 11. 9", s.form.dateText)
    }

    @Test
    fun `기록 필터는 기록이 있는 항목만, 고르면 그 항목 기록만, 다시 누르면 전체`() {
        val store = FakeStore(38_800.0)
        store.records.value = listOf(
            MaintenanceEntry(id = 2, date = today, odometerKm = 38_000, place = "", items = setOf(MaintenanceItemType.TIRE_ROTATION, MaintenanceItemType.BRAKE_FLUID)),
            MaintenanceEntry(id = 1, date = today.minusDays(30), odometerKm = 37_000, place = "", items = setOf(MaintenanceItemType.WIPER)),
        )
        store.emit(today)
        val vm = MaintenanceViewModel(today = today, snapshots = store.snapshots.filterNotNull())
        assertEquals(listOf("와이퍼 블레이드", "타이어 위치 교환", "브레이크액"), vm.uiState.value.filterOptions)
        assertEquals(2, vm.uiState.value.filteredRecords.size)

        vm.onSelectFilter("와이퍼 블레이드")
        assertEquals(listOf(1L), vm.uiState.value.filteredRecords.map { it.id })
        vm.onSelectFilter("와이퍼 블레이드")
        assertEquals(2, vm.uiState.value.filteredRecords.size)
        vm.onSelectFilter("브레이크액")
        vm.onSelectFilter(null)
        assertEquals(null, vm.uiState.value.recordFilter)
    }

    @Test
    fun `주행거리 0도 입력·저장, 지금 주행거리가 0이면 0으로 미리 채운다`() {
        assertEquals("0", MaintenanceViewModel.digitsOnly("000", 9))
        assertEquals("", MaintenanceViewModel.digitsOnly("km", 9))

        val store = FakeStore(0.0).apply { emit(today) }
        val vm = MaintenanceViewModel(
            today = today,
            snapshots = store.snapshots.filterNotNull(),
            saveRecord = { store.save(it); store.emit(today) },
        )
        assertEquals("0", vm.uiState.value.form.odometerDigits)
        vm.onToggleItem("와이퍼")
        assertTrue(vm.uiState.value.form.canSave)
        vm.onSave()
        assertEquals(0, store.saved.single().odometerKm)
    }
}
