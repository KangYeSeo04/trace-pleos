package com.tracers.trace.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TRACE 디자인 치수.
 *
 * 디자인 기준: 삼분할 오른쪽 칸 600 × 800 CSS px.
 * 변환: 디자인 1px = 1dp (scale = 1.0). 필요 시 scale로 일괄 조정.
 */
@Immutable
data class TraceDimens(
    /** 디자인 ↔ dp 변환 배율. 기본 1.0 (1px = 1dp) */
    val scale: Float = 1.0f,
) {
    /** 디자인 px → dp */
    fun px(value: Int): Dp = (value * scale).dp
    fun px(value: Float): Dp = (value * scale).dp

    /** 디자인 px → sp (글자 크기) */
    fun sp(value: Int): TextUnit = (value * scale).sp
    fun sp(value: Float): TextUnit = (value * scale).sp
}

val LocalTraceDimens = staticCompositionLocalOf { TraceDimens() }
