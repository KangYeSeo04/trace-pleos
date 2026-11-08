package com.tracers.trace.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * TRACE 다크 테마 색상 토큰.
 * CLAUDE.md의 "다크 테마 토큰" 표 기준.
 */
@Immutable
data class TraceColors(
    /** 화면 바탕 */
    val panel: Color = Color(0xFF17191D),
    /** 카드·타일 바탕 */
    val tile: Color = Color(0xFF202328),
    /** 주요 글자 */
    val t1: Color = Color(0xFFF2F3F5),
    /** 보조 글자 */
    val t2: Color = Color(0xFFA3A9B2),
    /** 흐린 글자, 라벨 */
    val t3: Color = Color(0xFF7C828B),
    /** 막대 바탕, 구분선 */
    val track: Color = Color(0xFF33373E),
    /** 강조 파랑 (유일한 강조색) */
    val accent: Color = Color(0xFF4C9AFF),
    /** 강조 파랑 반투명 (칩 배경) */
    val accentSoft: Color = Color(0x294C9AFF), // rgba(76,154,255,0.16)
    /** 좋음 */
    val good: Color = Color(0xFF38C08A),
    /** 좋음 반투명 (칩 배경) */
    val goodSoft: Color = Color(0x2438C08A), // rgba(56,192,138,0.14)
    /** 주의 */
    val caution: Color = Color(0xFFF0A33C),
    /** 경고 */
    val warn: Color = Color(0xFFEF7A70),
    /** 코인·별 */
    val gold: Color = Color(0xFFE9B949),
)

val LocalTraceColors = staticCompositionLocalOf { TraceColors() }
