package com.tracers.trace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 상세 화면 공통 헤더 (sticky).
 *
 * @param title 화면 제목
 * @param onBack 뒤로 버튼 클릭 콜백
 */
@Composable
fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.panel)
            .padding(horizontal = 20.dp, vertical = 0.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 뒤로 버튼
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.tile)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TraceIcons.ChevronLeft,
                contentDescription = "뒤로",
                tint = colors.t1,
                modifier = Modifier.size(22.dp)
            )
        }

        // 제목 (가운데)
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.t1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp)
        )
    }
}
