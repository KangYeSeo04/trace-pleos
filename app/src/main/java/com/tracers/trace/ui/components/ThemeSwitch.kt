package com.tracers.trace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 해/달 스위치 (다크 모드 토글).
 *
 * @param isDark 다크 모드 여부
 * @param onToggle 토글 콜백
 */
@Composable
fun ThemeSwitch(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileColor = TraceTheme.colors.tile
    val t2Color = TraceTheme.colors.t2

    // 손잡이 색상 (Home knobBg / knobFg)
    val knobBg = TraceTheme.colors.themeKnob
    val knobFg = TraceTheme.colors.themeKnobIcon
    val knobX = if (isDark) 36.dp else 4.dp

    Box(
        modifier = modifier
            .size(width = 72.dp, height = 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tileColor)
            .clickable(role = Role.Switch, onClick = onToggle)
    ) {
        // 해 아이콘 (왼쪽)
        Icon(
            imageVector = TraceIcons.Sun,
            contentDescription = null,
            tint = t2Color,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.CenterStart)
                .offset(x = 12.dp)
        )

        // 달 아이콘 (오른쪽)
        Icon(
            imageVector = TraceIcons.Moon,
            contentDescription = null,
            tint = t2Color,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp)
        )

        // 손잡이
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 4.dp)
                .size(32.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(knobBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) TraceIcons.Moon else TraceIcons.Sun,
                contentDescription = if (isDark) "다크 모드" else "라이트 모드",
                tint = knobFg,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
