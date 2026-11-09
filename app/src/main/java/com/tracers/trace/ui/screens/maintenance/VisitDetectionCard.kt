package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 방문 감지 카드 (S04-2, D-03).
 * 디자인: accentSoft 바탕, 모서리 20, 안쪽 20/20/18, 세로 간격 16.
 *
 * @param onRecord [기록하기] → S05 (날짜·장소 채움)
 * @param onDismiss [정비 안 했어요] → 카드 숨김
 */
@Composable
fun VisitDetectionCard(
    visit: VisitDetection,
    onRecord: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accentCard)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TraceIcons.MapPin,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = visit.title,
                    style = type.body(17f, FontWeight.Bold).copy(lineHeight = 1.4.em),
                    color = colors.t1
                )
                Text(
                    text = "정비를 받으셨다면 기록해 둘게요",
                    style = type.body(14f),
                    color = colors.t2
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VisitButton(
                text = "기록하기",
                background = colors.accent,
                textColor = colors.onAccent,
                onClick = onRecord,
                modifier = Modifier.weight(1f)
            )
            VisitButton(
                text = "정비 안 했어요",
                background = colors.panel,
                textColor = colors.t1,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 카드 버튼: 높이 52, 모서리 14, 16sp 600 */
@Composable
private fun VisitButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TraceTheme.type.body(16f, FontWeight.SemiBold),
            color = textColor
        )
    }
}
