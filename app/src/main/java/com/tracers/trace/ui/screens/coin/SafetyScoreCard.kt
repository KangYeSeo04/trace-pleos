package com.tracers.trace.ui.screens.coin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.components.LabelChip
import com.tracers.trace.ui.theme.TraceTheme
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 안전점수 카드 (S06-4). 디자인: 타일 모서리 20, 안쪽 22 22 16, 그래프 516 × 150.
 * null이면 주행 기록이 없다는 빈 카드.
 */
@Composable
internal fun SafetyScoreCard(chart: SafetyScoreChart?, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 16.dp)
    ) {
        if (chart == null) {
            Text(text = "아직 주행 기록이 없어요", style = type.body(17f, FontWeight.Bold), color = colors.t1)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "출발하면 1 km마다 점수가 바뀌어요. 80점 이상이면 코인이 쌓여요",
                style = type.body(14f),
                color = colors.t2
            )
            Spacer(Modifier.height(6.dp))
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatScore(chart.currentScore),
                    style = type.numberTight(40f),
                    color = colors.t1,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = "출발 때 ${formatScore(chart.startScore)}",
                    style = type.body(14f),
                    color = colors.t2,
                    modifier = Modifier.alignByBaseline()
                )
            }
            if (chart.isAccruing) {
                LabelChip(text = "적립 중", backgroundColor = colors.goodSoft, contentColor = colors.good)
            } else {
                LabelChip(text = "적립 멈춤", backgroundColor = colors.cautionSoft, contentColor = colors.caution)
            }
        }

        Spacer(Modifier.height(16.dp))
        ScoreLineChart(chart = chart)
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "출발", style = type.body(12f), color = colors.t3)
            Text(text = "점선 80점 · 아래로 내려가면 적립이 멈춰요", style = type.body(12f), color = colors.t3)
            Text(text = "${chart.distanceKm} km", style = type.body(12f), color = colors.t3)
        }
    }
}

/** 디자인 그래프 축: 78~92점을 y 140~10에, 0~거리 km를 x 2~514에 */
private object ScoreAxis {
    const val HEIGHT = 150f
    const val PAD_X = 2f
    fun y(score: Float): Float = (140f - (score - 78f) / 14f * 130f).roundToInt().toFloat().coerceIn(0f, HEIGHT)
}

@Composable
private fun ScoreLineChart(chart: SafetyScoreChart) {
    val colors = TraceTheme.colors
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ScoreAxis.HEIGHT.dp)
    ) {
        val d = density
        val w = size.width / d
        val total = chart.distanceKm.coerceAtLeast(1)
        fun x(k: Int): Float = (ScoreAxis.PAD_X + k.toFloat() / total * (w - ScoreAxis.PAD_X * 2)).roundToInt().toFloat()

        scale(d, d, pivot = Offset.Zero) {
            // 80점 점선 (t3, 1px, 3 4)
            val y80 = ScoreAxis.y(SafetyScoreChart.ACCRUAL_THRESHOLD)
            drawLine(
                color = colors.t3,
                start = Offset(0f, y80),
                end = Offset(w, y80),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f))
            )

            val line = Path()
            chart.scores.forEachIndexed { k, s ->
                if (k == 0) line.moveTo(x(k), ScoreAxis.y(s)) else line.lineTo(x(k), ScoreAxis.y(s))
            }
            // 아래 면 (accent 0.08)
            val area = Path().apply {
                addPath(line)
                lineTo(x(chart.distanceKm), ScoreAxis.HEIGHT)
                lineTo(x(0), ScoreAxis.HEIGHT)
                close()
            }
            drawPath(area, color = colors.accent.copy(alpha = 0.08f))
            drawPath(
                line,
                color = colors.accent,
                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 위험 km: 지름 8 주황 점 + 타일색 2px 테두리
            chart.riskKm.forEach { k ->
                val s = chart.scores.getOrNull(k) ?: return@forEach
                val c = Offset(x(k), ScoreAxis.y(s))
                drawCircle(colors.tile, radius = 6f, center = c)
                drawCircle(colors.caution, radius = 4f, center = c)
            }
        }
    }
}

private fun formatScore(v: Float): String = String.format(Locale.KOREA, "%.1f", v)
