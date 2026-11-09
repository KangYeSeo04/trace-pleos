package com.tracers.trace.ui.screens.efficiency

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.ui.components.DetailHeader
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.InterFontFamily
import com.tracers.trace.ui.theme.TraceTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * 전비 상세 화면 (S03).
 */
@Composable
fun EfficiencyScreen(
    uiState: EfficiencyUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors
    val scrollState = rememberScrollState()

    // 로컬 상태: 월별 전비에서 선택된 월
    var selectedMonth by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel)
    ) {
        // 헤더 (sticky)
        DetailHeader(
            title = "전비",
            onBack = onNavigateBack
        )

        // 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line)
        )

        // 스크롤 영역
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 90일 평균 섹션
            if (uiState.average90Days != null) {
                Average90DaysSection(uiState.average90Days)
            } else if (!uiState.isLoading) {
                EmptyDataMessage()
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 충전 타이밍 섹션
            SectionTitle("충전 타이밍")
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.chargeTiming != null) {
                ChargeTimingSection(uiState.chargeTiming)
            } else if (!uiState.isLoading) {
                EmptyDataMessage()
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 주행 기록 (달력)
            SectionTitle("주행 기록")
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.calendar != null) {
                DrivingCalendarSection(uiState.calendar)
            } else if (!uiState.isLoading) {
                EmptyDataMessage()
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 월별 전비
            SectionTitle("월별 전비")
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.monthlyEfficiency != null) {
                MonthlyEfficiencySection(
                    data = uiState.monthlyEfficiency,
                    selectedMonth = selectedMonth,
                    onMonthSelected = { month ->
                        selectedMonth = if (selectedMonth == month) null else month
                    },
                    onCloseDaily = { selectedMonth = null }
                )
            } else if (!uiState.isLoading) {
                EmptyDataMessage()
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 이번 달 예측
            SectionTitle("이번 달 예측")
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.prediction != null) {
                PredictionSection(uiState.prediction)
            } else if (!uiState.isLoading) {
                EmptyDataMessage()
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// ===== 90일 평균 섹션 =====

@Composable
private fun Average90DaysSection(average: EfficiencyAverage) {
    val colors = TraceTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(22.dp)
    ) {
        Text(
            text = if (average.isEstimated) "최근 90일 평균 · 추정" else "최근 90일 평균",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.t2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", average.avgEfficiency),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 48.sp,
                        letterSpacing = (-48 * 0.035f).sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = colors.t1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "km/kWh",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.t2
                )
            }

            if (average.todayComparison != null) {
                val comparison = average.todayComparison
                val chipBg = if (comparison.isAboveAverage) colors.goodSoft else colors.tile
                val chipColor = if (comparison.isAboveAverage) colors.good else colors.caution

                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(chipBg)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "오늘 ${String.format("%.1f", comparison.todayValue)} · 평균보다 ${String.format("%.1f", comparison.difference)} ${if (comparison.isAboveAverage) "높음" else "낮음"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = chipColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SummaryItem(
                "회생제동",
                // 10 kWh 미만은 소수 한 자리 (기록 초기에 0으로만 보이지 않게)
                String.format(if (average.summary.regenBraking < 10f) "%.1f" else "%.0f", average.summary.regenBraking),
                "kWh",
                Modifier.weight(1f)
            )
            SummaryItem(
                "공조",
                average.summary.hvac?.toString() ?: "—",
                if (average.summary.hvac != null) "%" else "",
                Modifier.weight(1f)
            )
            SummaryItem("급가속", "${average.summary.hardAccel}", "회", Modifier.weight(1f))
            SummaryItem("충전", "${average.summary.charges}", "회", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors

    Column(
        modifier = modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, fontSize = 13.sp, color = colors.t2)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = colors.t1
            )
            Text(text = " $unit", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.t2)
        }
    }
}

// ===== 충전 타이밍 섹션 =====

@Composable
private fun ChargeTimingSection(timing: ChargeTiming) {
    val colors = TraceTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = timing.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.t1)
            Text(text = timing.description, fontSize = 14.sp, color = colors.t2)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            timing.forecast.forEach { day ->
                ChargeForecastBar(day, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChargeForecastBar(day: ChargeForecastDay, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    val barColor = when {
        day.percentage < 20 -> colors.caution
        day.isToday -> colors.accent
        else -> colors.bar
    }
    val textColor = when {
        day.percentage < 20 -> colors.caution
        day.isToday -> colors.accent
        else -> colors.t2
    }
    val labelColor = if (day.isToday) colors.t1 else colors.t2
    val barHeight = (day.percentage * 1.2f).dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${day.percentage}%",
            style = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = textColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(barHeight).clip(RoundedCornerShape(8.dp)).background(barColor))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = day.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = labelColor)
    }
}

// ===== 주행 달력 섹션 =====

@Composable
private fun DrivingCalendarSection(calendar: DrivingCalendar) {
    val colors = TraceTheme.colors
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(14.dp)
    ) {
        // 헤더: 년월 + 이전/다음 버튼
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${calendar.year}년 ${calendar.month}월",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.t1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // 이전 달 버튼
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.panel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TraceIcons.ChevronLeft,
                        contentDescription = "이전 달",
                        tint = colors.t1,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // 다음 달 버튼
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.panel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TraceIcons.ChevronRight,
                        contentDescription = "다음 달",
                        tint = colors.t1,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 요일 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.t3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 달력 그리드 (첫 주 시작 요일 맞추기)
        val firstDayOfWeek = java.time.LocalDate.of(calendar.year, calendar.month, 1).dayOfWeek.value % 7
        val totalCells = firstDayOfWeek + calendar.days.size
        val rows = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayIndex = cellIndex - firstDayOfWeek

                        Box(modifier = Modifier.weight(1f)) {
                            if (dayIndex in calendar.days.indices) {
                                val day = calendar.days[dayIndex]
                                CalendarDayCell(
                                    day = day,
                                    isSelected = calendar.selectedDay == day.dayOfMonth
                                )
                            }
                        }
                    }
                }
            }
        }

        // 선택된 날짜 상세
        if (calendar.selectedDayDetail != null) {
            Spacer(modifier = Modifier.height(12.dp))
            DayDetailCard(calendar.selectedDayDetail)
        }
    }
}

@Composable
private fun CalendarDayCell(day: CalendarDay, isSelected: Boolean) {
    val colors = TraceTheme.colors
    val textColor = when {
        day.isFuture -> colors.t3
        else -> colors.t1
    }
    val bgColor = if (isSelected) colors.panel else Color.Transparent
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    val dotColor = when {
        day.hasCharging -> colors.accent
        day.hasDriving -> colors.t3
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .height(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(enabled = !day.isFuture) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${day.dayOfMonth}",
            style = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = fontWeight, fontFeatureSettings = "tnum"),
            color = textColor
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor))
    }
}

@Composable
private fun DayDetailCard(detail: DayDetail) {
    val colors = TraceTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.panel)
    ) {
        // 날짜 + 시간/거리
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 18.dp, 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = detail.dateLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.t1)
            Text(text = "${detail.durationMinutes}분 · ${String.format("%.1f", detail.distanceKm)} km", fontSize = 14.sp, color = colors.t2)
        }

        // 전비
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp, 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "전비", fontSize = 15.sp, color = colors.t2)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${String.format("%.1f", detail.efficiency)} km/kWh ",
                    style = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = colors.t1
                )
                Text(
                    text = "${if (detail.isAboveAverage) "+" else ""}${String.format("%.1f", detail.efficiencyDiff)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (detail.isAboveAverage) colors.good else colors.caution
                )
            }
        }

        // 충전 기록
        if (detail.chargingRecord != null) {
            val record = detail.chargingRecord
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp, 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${if (record.isFastCharging) "급속" else "완속"} 충전 ${record.timeRange}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.t1
                    )
                    Text(
                        text = "${record.stationName} · ${record.startPercent}% → ${record.endPercent}%",
                        fontSize = 13.sp,
                        color = colors.t2
                    )
                }
                Text(
                    text = "+${String.format("%.0f", record.addedKwh)} kWh",
                    style = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = colors.accent
                )
            }
        }
    }
}

// ===== 월별 전비 섹션 =====

@Composable
private fun MonthlyEfficiencySection(
    data: MonthlyEfficiency,
    selectedMonth: String?,
    onMonthSelected: (String) -> Unit,
    onCloseDaily: () -> Unit
) {
    val colors = TraceTheme.colors
    val avg = data.avg90Days

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(22.dp)
    ) {
        // 일별 꺾은선 (선택된 월이 있을 때)
        if (selectedMonth != null && data.dailyChart != null) {
            DailyEfficiencyChartSection(
                chart = data.dailyChart,
                onClose = onCloseDaily
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 6개월 막대
        Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
            // 평균 점선
            val avgBarHeight = ((avg - 4f) / 3f * 115f).coerceIn(0f, 115f)
            val avgLineY = 190f - 8f - 18f - 7f - avgBarHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = colors.t3,
                    start = Offset(0f, avgLineY.dp.toPx()),
                    end = Offset(size.width, avgLineY.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.months.forEach { month ->
                    MonthBarItem(
                        month = month,
                        avg90Days = avg,
                        isSelected = selectedMonth == month.label,
                        onClick = { onMonthSelected(month.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 범례
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors.accent))
                Text(text = "평균 이상", fontSize = 13.sp, color = colors.t2)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors.caution))
                Text(text = "평균 미만", fontSize = 13.sp, color = colors.t2)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.width(14.dp).height(1.dp).background(colors.t3))
                Text(text = "90일 평균 ${String.format("%.1f", avg)}", fontSize = 13.sp, color = colors.t2)
            }
        }
    }
}

@Composable
private fun MonthBarItem(
    month: MonthBar,
    avg90Days: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors
    val isAboveAvg = month.avgEfficiency >= avg90Days
    val barColor = if (isAboveAvg) colors.accent else colors.caution
    val textColor = if (isAboveAvg) colors.t1 else colors.caution
    val bgColor = if (isSelected) colors.panel else Color.Transparent
    val barHeight = ((month.avgEfficiency - 4f) / 3f * 115f).coerceIn(0f, 115f).dp

    Column(
        modifier = modifier
            .height(190.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = String.format("%.1f", month.avgEfficiency),
            style = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = textColor
        )
        Spacer(modifier = Modifier.height(7.dp))
        Box(modifier = Modifier.width(26.dp).height(barHeight).clip(RoundedCornerShape(7.dp)).background(barColor))
        Spacer(modifier = Modifier.height(7.dp))
        Text(text = month.label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.t2)
    }
}

@Composable
private fun DailyEfficiencyChartSection(
    chart: DailyEfficiencyChart,
    onClose: () -> Unit
) {
    val colors = TraceTheme.colors
    val scrollState = rememberScrollState()

    // 차트 크기 계산 (디자인 기준)
    val chartWidth = (18 + (chart.dailyData.size - 1) * 36 + 18).dp
    val chartHeight = 162.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(-4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.panel)
            .padding(top = 18.dp, bottom = 16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = chart.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.t1)
                Text(text = chart.subtitle, fontSize = 13.sp, color = colors.t2)
            }
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.tile).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = TraceIcons.Close, contentDescription = "닫기", tint = colors.t1, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 가로 스크롤 차트
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 22.dp)
        ) {
            Box(modifier = Modifier.width(chartWidth).height(chartHeight)) {
                DailyLineChart(
                    data = chart.dailyData,
                    avg90Days = chart.avg90Days,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun DailyLineChart(
    data: List<DailyEfficiency>,
    avg90Days: Float,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    // Y 좌표 계산 함수 (디자인 기준)
    fun yOf(v: Float): Float = 30f + (7.4f - v.coerceIn(3.8f, 7.4f)) / 3.6f * 96f
    fun xOf(i: Int): Float = 18f + (i - 1) * 36f

    // 최고/최저 찾기
    val maxDay = data.maxByOrNull { it.efficiency }
    val minDay = data.minByOrNull { it.efficiency }

    Canvas(modifier = modifier) {
        val density = this
        val dpToPx = { dp: Float -> dp * density.density }

        // 평균 점선
        val avgY = dpToPx(yOf(avg90Days))
        drawLine(
            color = colors.t3,
            start = Offset(0f, avgY),
            end = Offset(size.width, avgY),
            strokeWidth = dpToPx(1f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dpToPx(4f), dpToPx(4f)))
        )

        // 데이터가 있을 때만 그래프 그림
        if (data.isNotEmpty()) {
            // 영역 채움
            val areaPath = Path().apply {
                moveTo(dpToPx(xOf(1)), dpToPx(yOf(data[0].efficiency)))
                data.forEachIndexed { index, d ->
                    lineTo(dpToPx(xOf(index + 1)), dpToPx(yOf(d.efficiency)))
                }
                lineTo(dpToPx(xOf(data.size)), dpToPx(130f))
                lineTo(dpToPx(xOf(1)), dpToPx(130f))
                close()
            }
            drawPath(areaPath, color = colors.accent.copy(alpha = 0.08f))

            // 선
            val linePath = Path().apply {
                moveTo(dpToPx(xOf(1)), dpToPx(yOf(data[0].efficiency)))
                data.forEachIndexed { index, d ->
                    lineTo(dpToPx(xOf(index + 1)), dpToPx(yOf(d.efficiency)))
                }
            }
            drawPath(
                linePath,
                color = colors.accent,
                style = Stroke(
                    width = dpToPx(2.5f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 점
            data.forEachIndexed { index, d ->
                val x = dpToPx(xOf(index + 1))
                val y = dpToPx(yOf(d.efficiency))
                val dotColor = if (d.efficiency >= avg90Days) colors.accent else colors.caution

                drawCircle(color = colors.panel, radius = dpToPx(5f), center = Offset(x, y))
                drawCircle(color = dotColor, radius = dpToPx(4f), center = Offset(x, y))
            }
        }
    }

    // 날짜 라벨 (Canvas 위에 Text로 그리기는 복잡하므로 별도 Row로 처리)
}

// ===== 이번 달 예측 섹션 =====

@Composable
private fun PredictionSection(prediction: MonthPrediction) {
    val colors = TraceTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        // 평소 완충 시
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp, 18.dp, 22.dp, 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "평소 완충 시", fontSize = 15.sp, color = colors.t1)
            Text(
                text = "${prediction.normalRangeKm} km",
                style = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = colors.t2
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))

        // 이번 달 완충 시
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp, 18.dp, 22.dp, 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${prediction.monthLabel} 완충 시", fontSize = 15.sp, color = colors.t1)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "약 ${prediction.predictedRangeKm} km ",
                    style = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = colors.t1
                )
                Text(
                    text = "${prediction.changePercent}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (prediction.changePercent < 0) colors.caution else colors.good
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.line))

        // 팁
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp, 16.dp, 22.dp, 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = TraceIcons.Info,
                contentDescription = null,
                tint = colors.t2,
                modifier = Modifier.size(18.dp).padding(top = 1.dp)
            )
            Text(
                text = prediction.tip,
                fontSize = 14.sp,
                color = colors.t2,
                lineHeight = 21.sp
            )
        }
    }
}

// ===== 공통 =====

@Composable
private fun SectionTitle(text: String) {
    val colors = TraceTheme.colors
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.t2,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun EmptyDataMessage() {
    val colors = TraceTheme.colors
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.tile).padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "데이터를 모으는 중이에요", fontSize = 15.sp, color = colors.t2)
    }
}

// ===== Preview =====

private fun createSampleCalendar(): DrivingCalendar {
    val days = (1..30).map { day ->
        CalendarDay(
            dayOfMonth = day,
            hasDriving = day <= 6,
            hasCharging = day == 2 || day == 5,
            isFuture = day > 6,
            isToday = day == 6
        )
    }
    return DrivingCalendar(
        year = 2026,
        month = 11,
        days = days,
        selectedDay = 2,
        selectedDayDetail = DayDetail(
            dateLabel = "11월 2일 월요일",
            durationMinutes = 58,
            distanceKm = 42.6f,
            efficiency = 6.4f,
            efficiencyDiff = 1.1f,
            isAboveAverage = true,
            chargingRecord = ChargingRecord(
                timeRange = "08:10 – 08:52",
                isFastCharging = true,
                stationName = "[충전소 이름]",
                startPercent = 31,
                endPercent = 64,
                addedKwh = 28f
            )
        )
    )
}

private fun createSampleMonthlyEfficiency(): MonthlyEfficiency {
    val months = listOf(
        MonthBar("5월", 6.2f),
        MonthBar("6월", 6.4f),
        MonthBar("7월", 5.1f),
        MonthBar("8월", 5.0f),
        MonthBar("9월", 6.1f),
        MonthBar("10월", 5.6f)
    )

    // 10월 일별 데이터 생성 (디자인 공식 적용)
    val dailyData = (1..31).map { day ->
        val seed = 3 * 7 + 5.6f * 13
        val v = (5.6f + 0.75f * sin(day * 1.7f + seed) + 0.35f * cos(day * 0.83f + seed * 0.5f))
        DailyEfficiency(day, (v * 10).toInt() / 10f)
    }

    val maxD = dailyData.maxByOrNull { it.efficiency }!!
    val minD = dailyData.minByOrNull { it.efficiency }!!

    return MonthlyEfficiency(
        months = months,
        avg90Days = 5.8f,
        selectedMonth = "10월",
        dailyChart = DailyEfficiencyChart(
            title = "10월 일별 전비",
            subtitle = "월 평균 5.6 · 최고 ${maxD.day}일 ${String.format("%.1f", maxD.efficiency)} · 최저 ${String.format("%.1f", minD.efficiency)}",
            dailyData = dailyData,
            avg90Days = 5.8f
        )
    )
}

@Preview(widthDp = 600, heightDp = 2000, showBackground = true)
@Composable
private fun EfficiencyScreenPreview() {
    TraceTheme {
        EfficiencyScreen(
            uiState = EfficiencyUiState(
                average90Days = EfficiencyAverage(
                    avgEfficiency = 5.8f,
                    todayComparison = TodayComparison(6.4f, 1.1f, true),
                    summary = EfficiencySummary(38f, 17, 4, 11)
                ),
                chargeTiming = ChargeTiming(
                    title = "목요일까지 충분해요",
                    description = "배터리 64% · 하루 평균 34 km 기준",
                    forecast = listOf(
                        ChargeForecastDay("지금", 64, true),
                        ChargeForecastDay("내일", 53, false),
                        ChargeForecastDay("수", 42, false),
                        ChargeForecastDay("목", 31, false),
                        ChargeForecastDay("금", 19, false)
                    )
                ),
                calendar = createSampleCalendar(),
                monthlyEfficiency = createSampleMonthlyEfficiency(),
                prediction = MonthPrediction(
                    monthLabel = "11월",
                    normalRangeKm = 380,
                    predictedRangeKm = 323,
                    changePercent = -15,
                    tip = "히터 사용이 늘어 주행거리가 줄어요. 출발 10분 전 예약 공조를 켜 두면 덜 줄어들어요."
                )
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun EfficiencyScreenEmptyPreview() {
    TraceTheme {
        EfficiencyScreen(
            uiState = EfficiencyUiState(),
            onNavigateBack = {}
        )
    }
}
