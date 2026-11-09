package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceColors
import com.tracers.trace.ui.theme.TraceTheme
import java.time.LocalDate

private val WEEK_DAYS = listOf("일", "월", "화", "수", "목", "금", "토")

/**
 * 정비 달력 카드 + 고른 날 기록 카드. 디자인: 타일 바탕, 모서리 20, 안쪽 14/14/18.
 */
@Composable
fun MaintenanceCalendarCard(
    calendar: MaintenanceCalendar,
    records: List<MaintenanceRecord>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    val month = calendar.month
    val recordDays = MaintenanceCalendarLogic.recordsIn(month, records).map { it.date.dayOfMonth }.toSet()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 18.dp)
    ) {
        // 년월 + 이전/다음 (안쪽 0 4 0 8)
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${month.year}년 ${month.monthValue}월",
                style = type.body(17f, FontWeight.Bold),
                color = colors.t1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MonthButton(TraceIcons.ChevronLeft, "이전 달", onPrevMonth)
                MonthButton(TraceIcons.ChevronRight, "다음 달", onNextMonth)
            }
        }

        // 요일 (위 12, 12sp 500 t3)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            WEEK_DAYS.forEach {
                Text(
                    text = it,
                    style = type.body(12f, FontWeight.Medium),
                    color = colors.t3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 날짜 칸 (위 4, 간격 2, 높이 54)
        val first = month.atDay(1).dayOfWeek.value % 7
        val days = month.lengthOfMonth()
        val rows = (first + days + 6) / 7
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (c in 0 until 7) {
                        val d = r * 7 + c - first + 1
                        val cell = Modifier.weight(1f).height(54.dp)
                        if (d in 1..days) {
                            val date = month.atDay(d)
                            DayCell(
                                day = d,
                                monthValue = month.monthValue,
                                hasRecord = d in recordDays,
                                selected = date == calendar.selectedDate,
                                onClick = { onSelectDate(date) },
                                modifier = cell
                            )
                        } else {
                            Spacer(cell)
                        }
                    }
                }
            }
        }

        DayRecordsCard(
            card = MaintenanceCalendarLogic.dayCard(calendar, records),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp)
        )
    }
}

/** 44 × 44 원형, 패널 바탕, 화살표 18 */
@Composable
private fun MonthButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = TraceTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.panel)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = colors.t1, modifier = Modifier.size(18.dp))
    }
}

/** 날짜 칸: 모서리 14, 고른 날 패널 바탕, 기록 있는 날 굵게 + 파란 점 5 */
@Composable
private fun DayCell(
    day: Int,
    monthValue: Int,
    hasRecord: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.panel else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${monthValue}월 ${day}일" + if (hasRecord) ", 정비 기록 있음" else ""
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = day.toString(),
            style = TraceTheme.type.number(15f, if (selected || hasRecord) FontWeight.Bold else FontWeight.Medium)
                .copy(letterSpacing = TraceTheme.type.body(15f).letterSpacing),
            color = if (selected || hasRecord) colors.t1 else colors.t3
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (hasRecord) colors.accent else Color.Transparent)
        )
    }
}

/** 고른 날 기록 카드: 패널 바탕, 모서리 16 */
@Composable
private fun DayRecordsCard(card: MaintenanceCalendarLogic.DayCard, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.panel)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(card.title, style = type.body(16f, FontWeight.Bold), color = colors.t1)
            Text(card.count, style = type.body(14f), color = colors.t2)
        }
        card.records.forEach { rec ->
            RowDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(rec.title, style = type.body(15f, FontWeight.SemiBold), color = colors.t1)
                    Text(recordSubline(rec, colors), style = type.body(13f), color = colors.t2)
                }
                rec.costWon?.let { Text(formatWon(it), style = type.body(14f), color = colors.t2) }
            }
        }
        if (card.records.isEmpty()) {
            RowDivider()
            Text(
                text = card.emptyText,
                style = type.body(14f),
                color = colors.t2,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
internal fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(TraceTheme.colors.line))
}

/** "장소 · 30,900 km · 자동 감지" (입력 방식만 색: 자동 감지 파랑, 직접 입력 t2) */
internal fun recordSubline(rec: MaintenanceRecord, colors: TraceColors): AnnotatedString = buildAnnotatedString {
    append("${rec.place} · ${formatKm(rec.odometerKm)} · ")
    withStyle(SpanStyle(color = if (rec.method == RecordInputMethod.AutoDetected) colors.accent else colors.t2)) {
        append(rec.method.label)
    }
}
