package com.tracers.trace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * TRACE 테마.
 *
 * 이번 단계는 다크 모드만 구현. 라이트 모드는 나중에 토큰만 추가.
 * Material3는 쓰되 기본 테마 색은 쓰지 않는다 (TraceColors 사용).
 */
@Composable
fun TraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = TraceColors()
    val type = TraceType()
    val dimens = TraceDimens()

    // Material3 색상은 최소한만 설정 (배경, 표면)
    val materialColors = darkColorScheme(
        background = colors.panel,
        surface = colors.tile,
        onBackground = colors.t1,
        onSurface = colors.t1,
        primary = colors.accent,
        onPrimary = colors.t1,
    )

    CompositionLocalProvider(
        LocalTraceColors provides colors,
        LocalTraceType provides type,
        LocalTraceDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            content = content
        )
    }
}

/**
 * 현재 테마 색상에 접근.
 * 사용: TraceTheme.colors.panel
 */
object TraceTheme {
    val colors: TraceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceColors.current

    val type: TraceType
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceType.current

    val dimens: TraceDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceDimens.current
}
