package com.tracers.trace.ui.screens.efficiency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.ui.components.DetailHeader
import com.tracers.trace.ui.theme.InterFontFamily
import com.tracers.trace.ui.theme.TraceTheme

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

            // TODO(2회차): 주행 기록 (달력)
            SectionTitle("주행 기록")
            Spacer(modifier = Modifier.height(10.dp))
            PlaceholderBox("TODO: 달력 (2회차)")

            Spacer(modifier = Modifier.height(26.dp))

            // TODO(2회차): 월별 전비
            SectionTitle("월별 전비")
            Spacer(modifier = Modifier.height(10.dp))
            PlaceholderBox("TODO: 월별 그래프 (2회차)")

            Spacer(modifier = Modifier.height(26.dp))

            // TODO(2회차): 이번 달 예측
            SectionTitle("이번 달 예측")
            Spacer(modifier = Modifier.height(10.dp))
            PlaceholderBox("TODO: 예측 (2회차)")
        }
    }
}

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
        // 라벨
        Text(
            text = "최근 90일 평균",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.t2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 평균 값 + 비교 칩
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // 큰 숫자
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

            // 비교 칩
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
                    val sign = if (comparison.isAboveAverage) "+" else ""
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

        // 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line)
        )

        // 요약 4칸
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SummaryItem(
                label = "회생제동",
                value = String.format("%.0f", average.summary.regenBraking),
                unit = "kWh",
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = "공조",
                value = "${average.summary.hvac}",
                unit = "%",
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = "급가속",
                value = "${average.summary.hardAccel}",
                unit = "회",
                modifier = Modifier.weight(1f)
            )
            SummaryItem(
                label = "충전",
                value = "${average.summary.charges}",
                unit = "회",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    Column(
        modifier = modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.t2
        )
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
            Text(
                text = " $unit",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.t2
            )
        }
    }
}

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
        // 제목 + 설명
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = timing.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.t1
            )
            Text(
                text = timing.description,
                fontSize = 14.sp,
                color = colors.t2
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5일 막대
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
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

    // 막대 색: 20% 미만은 주의색, 오늘은 파랑, 나머지는 track
    val barColor = when {
        day.percentage < 20 -> colors.caution
        day.isToday -> colors.accent
        else -> colors.track
    }

    // 글자 색: 20% 미만은 주의색, 오늘은 파랑, 나머지는 t2
    val textColor = when {
        day.percentage < 20 -> colors.caution
        day.isToday -> colors.accent
        else -> colors.t2
    }

    val labelColor = if (day.isToday) colors.t1 else colors.t2

    // 막대 높이 (140dp 기준, 1% = 1.2dp)
    val barHeight = (day.percentage * 1.2f).dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 퍼센트
        Text(
            text = "${day.percentage}%",
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 막대
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(barColor)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 라벨
        Text(
            text = day.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
    }
}

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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "데이터를 모으는 중이에요",
            fontSize = 15.sp,
            color = colors.t2
        )
    }
}

@Composable
private fun PlaceholderBox(text: String) {
    val colors = TraceTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = colors.t3
        )
    }
}

// ===== Preview =====

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun EfficiencyScreenPreview() {
    TraceTheme {
        EfficiencyScreen(
            uiState = EfficiencyUiState(
                average90Days = EfficiencyAverage(
                    avgEfficiency = 5.8f,
                    todayComparison = TodayComparison(
                        todayValue = 6.4f,
                        difference = 1.1f,
                        isAboveAverage = true
                    ),
                    summary = EfficiencySummary(
                        regenBraking = 38f,
                        hvac = 17,
                        hardAccel = 4,
                        charges = 11
                    )
                ),
                chargeTiming = ChargeTiming(
                    title = "목요일까지 충분해요",
                    description = "배터리 64% · 하루 평균 34 km 기준",
                    forecast = listOf(
                        ChargeForecastDay("지금", 64, isToday = true),
                        ChargeForecastDay("내일", 53, isToday = false),
                        ChargeForecastDay("수", 42, isToday = false),
                        ChargeForecastDay("목", 31, isToday = false),
                        ChargeForecastDay("금", 19, isToday = false)
                    )
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
            uiState = EfficiencyUiState(
                average90Days = null,
                chargeTiming = null
            ),
            onNavigateBack = {}
        )
    }
}
