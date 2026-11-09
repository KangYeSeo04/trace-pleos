package com.tracers.trace.ui.screens.maintenance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MaintenanceCalendarLogicTest {

    private fun rec(date: String) = MaintenanceRecord(
        id = 0, date = LocalDate.parse(date), odometerKm = 0, title = "t", place = "p",
        method = RecordInputMethod.Manual, itemNames = emptyList(),
    )

    private val records = listOf(rec("2026-05-14"), rec("2026-05-02"), rec("2026-01-08"))
    private val today = LocalDate.of(2026, 11, 9)

    @Test
    fun `처음엔 가장 최근 기록의 달과 그날`() {
        val cal = MaintenanceCalendarLogic.initial(records, today)
        assertEquals(YearMonth.of(2026, 5), cal.month)
        assertEquals(LocalDate.of(2026, 5, 14), cal.selectedDate)
    }

    @Test
    fun `기록이 없으면 이번 달, 고른 날 없음`() {
        val cal = MaintenanceCalendarLogic.initial(emptyList(), today)
        assertEquals(YearMonth.of(2026, 11), cal.month)
        assertNull(cal.selectedDate)
        assertEquals("이 달에는 정비 기록이 없어요", MaintenanceCalendarLogic.dayCard(cal, emptyList()).emptyText)
    }

    @Test
    fun `달을 옮기면 고른 날이 지워지고 달 요약이 보인다`() {
        val cal = MaintenanceCalendarLogic.move(MaintenanceCalendarLogic.initial(records, today), -4)
        assertEquals(YearMonth.of(2026, 1), cal.month)
        val card = MaintenanceCalendarLogic.dayCard(cal, records)
        assertEquals("2026년 1월", card.title)
        assertEquals("정비 1건", card.count)
    }

    @Test
    fun `고른 날 제목은 요일까지, 기록 없으면 건수 비움`() {
        val card = MaintenanceCalendarLogic.dayCard(
            MaintenanceCalendar(YearMonth.of(2026, 5), LocalDate.of(2026, 5, 14)), records
        )
        assertEquals("5월 14일 목요일", card.title)
        assertEquals("정비 1건", card.count)

        val none = MaintenanceCalendarLogic.dayCard(
            MaintenanceCalendar(YearMonth.of(2026, 5), LocalDate.of(2026, 5, 15)), records
        )
        assertEquals("", none.count)
        assertEquals("이 날은 정비 기록이 없어요", none.emptyText)
    }

    @Test
    fun `금액·거리 천 단위 쉼표`() {
        assertEquals("30,900 km", formatKm(30_900))
        assertEquals("45,000원", formatWon(45_000))
    }
}
