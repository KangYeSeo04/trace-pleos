package com.tracers.trace.ui.screens.maintenance

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 정비 달력 규칙. MaintDark.dc.html renderVals()의 달력 부분을 옮겼다.
 */
object MaintenanceCalendarLogic {

    /** 처음 보여 줄 달: 가장 최근 기록의 달과 그날. 기록이 없으면 이번 달, 고른 날 없음 */
    fun initial(records: List<MaintenanceRecord>, today: LocalDate): MaintenanceCalendar {
        val latest = records.maxByOrNull { it.date } ?: return MaintenanceCalendar(YearMonth.from(today))
        return MaintenanceCalendar(YearMonth.from(latest.date), latest.date)
    }

    /** 이전·다음 달로. 고른 날은 지운다 */
    fun move(calendar: MaintenanceCalendar, months: Long): MaintenanceCalendar =
        MaintenanceCalendar(calendar.month.plusMonths(months), selectedDate = null)

    fun recordsIn(month: YearMonth, records: List<MaintenanceRecord>): List<MaintenanceRecord> =
        records.filter { YearMonth.from(it.date) == month }

    /** 달력 아래 카드 내용 */
    fun dayCard(calendar: MaintenanceCalendar, records: List<MaintenanceRecord>): DayCard {
        val inMonth = recordsIn(calendar.month, records)
        val sel = calendar.selectedDate
        if (sel == null) {
            return DayCard(
                title = "${calendar.month.year}년 ${calendar.month.monthValue}월",
                count = "정비 ${inMonth.size}건",
                records = emptyList(),
                emptyText = if (inMonth.isNotEmpty()) "점이 찍힌 날을 누르면 그날 정비 기록을 볼 수 있어요"
                else "이 달에는 정비 기록이 없어요",
            )
        }
        val dayRecs = inMonth.filter { it.date == sel }
        val weekday = sel.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        return DayCard(
            title = "${sel.monthValue}월 ${sel.dayOfMonth}일 $weekday",
            count = if (dayRecs.isNotEmpty()) "정비 ${dayRecs.size}건" else "",
            records = dayRecs,
            emptyText = "이 날은 정비 기록이 없어요",
        )
    }

    /**
     * @param emptyText 기록이 없을 때 문구. records가 비어 있을 때만 보인다
     */
    data class DayCard(
        val title: String,
        val count: String,
        val records: List<MaintenanceRecord>,
        val emptyText: String,
    )
}

/** 1,234 형식 */
internal fun formatThousands(value: Long): String = String.format(Locale.KOREA, "%,d", value)

/** "30,900 km" */
internal fun formatKm(km: Int): String = formatThousands(km.toLong()) + " km"

/** "45,000원" */
internal fun formatWon(won: Int): String = formatThousands(won.toLong()) + "원"
