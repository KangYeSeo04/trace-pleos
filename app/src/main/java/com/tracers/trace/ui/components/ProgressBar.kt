package com.tracers.trace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 진행 막대 (눈금 포함 가능).
 *
 * @param percent 채움 비율 (0~100)
 * @param barColor 막대 색상
 * @param markPercent 눈금 위치 (null이면 눈금 없음)
 * @param trackColor 막대 바탕색 (기본 track)
 */
@Composable
fun ProgressBar(
    percent: Float,
    barColor: Color,
    markPercent: Float? = null,
    modifier: Modifier = Modifier,
    trackColor: Color = TraceTheme.colors.track,
) {
    val markColor = TraceTheme.colors.t2

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(trackColor)
    ) {
        // 채움 막대
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

        // 눈금
        if (markPercent != null) {
            Box(
                modifier = Modifier
                    .offset(x = (markPercent / 100f).dp * 0 - 1.dp, y = (-3).dp)
                    .width(2.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(markColor)
            )
        }
    }
}
