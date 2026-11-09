package com.tracers.trace.domain

import com.tracers.trace.domain.MaintenanceItemType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceCalculatorTest {

    private val today = LocalDate.of(2026, 11, 9)
    private val baseline = MaintenanceBaseline(date = LocalDate.of(2026, 11, 1), odometerKm = 30_000.0)

    private fun record(date: LocalDate, km: Int, vararg items: MaintenanceItemType) =
        MaintenanceEntry(date = date, odometerKm = km, place = "", items = items.toSet())

    private fun calc(records: List<MaintenanceEntry> = emptyList(), odo: Double? = 31_000.0, base: MaintenanceBaseline? = baseline) =
        MaintenanceCalculator.calculate(records, base, today, odo)

    @Test
    fun `기록이 없으면 8개 모두 처음 본 날짜·주행거리를 기준으로 센다`() {
        val list = calc()
        assertEquals(8, list.size)
        assertTrue(list.all { it.basis == MaintenanceBasis.FIRST_SEEN })
        val tire = list.first { it.type == TIRE_ROTATION }
        assertEquals(1_000.0, tire.km!!.usedKm, 0.001)
        assertEquals(9_000.0, tire.km!!.remainingKm, 0.001)
        assertEquals(0.10, tire.usageRatio, 1e-9)
    }

    @Test
    fun `기준점도 기록도 없으면 빈 목록`() {
        assertTrue(calc(base = null).isEmpty())
    }

    @Test
    fun `기준점이 없어도 기록이 있는 항목은 센다`() {
        val list = calc(listOf(record(LocalDate.of(2026, 1, 1), 25_000, CABIN_FILTER)), base = null)
        assertEquals(listOf(CABIN_FILTER), list.map { it.type })
    }

    @Test
    fun `마지막 정비 기록 기준으로 남은 거리`() {
        val records = listOf(
            record(LocalDate.of(2025, 1, 1), 20_000, TIRE_ROTATION),
            record(LocalDate.of(2026, 5, 14), 24_000, TIRE_ROTATION),
        )
        val tire = calc(records).first { it.type == TIRE_ROTATION }
        assertEquals(MaintenanceBasis.LAST_RECORD, tire.basis)
        assertEquals(LocalDate.of(2026, 5, 14), tire.basisDate)
        assertEquals(7_000.0, tire.km!!.usedKm, 0.001)
        assertEquals(3_000.0, tire.km!!.remainingKm, 0.001)
        assertEquals(0.70, tire.usageRatio, 1e-9)
    }

    @Test
    fun `기간 기준은 마지막 기록일에 주기 개월을 더한 날까지`() {
        // 와이퍼 12개월: 2025-11-20 → 2026-11-20, 남은 11일
        val w = calc(listOf(record(LocalDate.of(2025, 11, 20), 24_600, WIPER))).first { it.type == WIPER }
        val t = w.time!!
        assertEquals(LocalDate.of(2026, 11, 20), t.dueDate)
        assertEquals(11L, t.remainingDays)
        assertEquals(354.0 / 365.0, w.usageRatio, 1e-9)
        assertEquals(11.6, t.elapsedMonths, 0.05)
        assertNull(w.km)
    }

    @Test
    fun `지난 항목은 남은 값이 음수, 사용률 100 넘음`() {
        val w = calc(listOf(record(LocalDate.of(2025, 10, 9), 20_000, WIPER))).first { it.type == WIPER }
        assertEquals(-31L, w.time!!.remainingDays)
        assertTrue(w.usageRatio > 1.0)
        assertEquals(MaintenanceLevel.WARN, w.level)
    }

    @Test
    fun `사용률 90 이상 경고, 75 이상 주의, 그 외 보통`() {
        fun levelAt(usedKm: Int) = calc(listOf(record(today, 31_000 - usedKm, CABIN_FILTER))).first { it.type == CABIN_FILTER }.level
        assertEquals(MaintenanceLevel.WARN, levelAt(9_000))
        assertEquals(MaintenanceLevel.CAUTION, levelAt(8_999))
        assertEquals(MaintenanceLevel.CAUTION, levelAt(7_500))
        assertEquals(MaintenanceLevel.NORMAL, levelAt(7_499))
    }

    @Test
    fun `급한 순 정렬, 사용률이 같으면 항목 순서`() {
        val records = listOf(
            record(LocalDate.of(2026, 1, 8), 22_200, CABIN_FILTER),   // 8,800 / 10,000 = 88%
            record(LocalDate.of(2025, 11, 20), 24_600, WIPER),        // 97%
            record(LocalDate.of(2026, 5, 14), 23_100, TIRE_ROTATION), // 79%
        )
        val list = calc(records)
        assertEquals(listOf(WIPER, CABIN_FILTER, TIRE_ROTATION), list.take(3).map { it.type })
        assertEquals(list.sortedByDescending { it.usageRatio }, list)

        // 처음 본 기준이면 에어컨 필터·타이어 위치 교환 둘 다 10,000 km 중 1,000 km(10%) → 정의 순서
        assertEquals(listOf(CABIN_FILTER, TIRE_ROTATION), calc().take(2).map { it.type })
    }

    @Test
    fun `거리·기간 둘 다면 먼저 오는 쪽(사용률 큰 쪽)을 쓴다`() {
        val s = MaintenanceCalculator.statusOf(
            type = WIPER, intervalKm = 10_000, intervalMonths = 12,
            basis = MaintenanceBasis.LAST_RECORD, basisDate = LocalDate.of(2026, 5, 9), basisOdometerKm = 30_000.0,
            today = today, odometerKm = 39_500.0,
        )
        assertEquals(0.95, s.km!!.ratio, 1e-9)
        assertTrue(s.time!!.ratio < 0.6)
        assertEquals(0.95, s.usageRatio, 1e-9)
        assertEquals(MaintenanceMeasure.KM, s.leading)
    }

    @Test
    fun `주행거리를 모르면 거리 기준은 비워 두고 맨 뒤로`() {
        val list = calc(listOf(record(LocalDate.of(2025, 11, 20), 24_600, WIPER)), odo = null)
        val filter = list.first { it.type == CABIN_FILTER }
        assertNull(filter.km)
        assertNull(filter.leading)
        assertEquals(WIPER, list.first().type)
        assertEquals(CABIN_FILTER, list.drop(1).first { it.leading == null }.type)
    }

    @Test
    fun `주행거리가 기록보다 작아도 사용은 0 아래로 내려가지 않는다`() {
        val f = calc(listOf(record(today, 40_000, CABIN_FILTER))).first { it.type == CABIN_FILTER }
        assertEquals(0.0, f.km!!.usedKm, 0.0)
    }

    @Test
    fun `14일 또는 500km 이내면 점검 임박`() {
        fun wiperDue(remainingDays: Long) =
            calc(listOf(record(today.plusDays(remainingDays).minusMonths(12), 30_000, WIPER))).first { it.type == WIPER }
        assertTrue(wiperDue(14).isImminent)
        assertFalse(wiperDue(15).isImminent)
        assertTrue(wiperDue(-3).isImminent)

        fun filterLeft(km: Int) = calc(listOf(record(today, 31_000 - (10_000 - km), CABIN_FILTER))).first { it.type == CABIN_FILTER }
        assertTrue(filterLeft(500).isImminent)
        assertFalse(filterLeft(501).isImminent)
    }

    @Test
    fun `가장 급한 점검 임박 항목`() {
        val records = listOf(
            record(today.minusMonths(12).plusDays(10), 30_000, WIPER),  // 10일 남음
            record(today, 21_200, CABIN_FILTER),                        // 200 km 남음, 98%
        )
        val list = calc(records)
        assertEquals(CABIN_FILTER, MaintenanceCalculator.mostImminent(list)?.type)
        assertNull(MaintenanceCalculator.mostImminent(calc()))
    }
}
