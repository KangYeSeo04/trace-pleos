package com.tracers.trace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.min

/** 디자인 기준 프레임 크기 (CSS px = dp) */
private const val DESIGN_WIDTH_DP = 600f
private const val DESIGN_HEIGHT_DP = 800f

/**
 * TRACE 테마.
 *
 * 이번 단계는 다크 모드만 구현. 라이트 모드는 나중에 토큰만 추가.
 * Material3는 쓰되 기본 테마 색은 쓰지 않는다 (TraceColors 사용).
 *
 * scale = min(칸가로/600, 칸세로/800)로 동적 계산.
 * LocalDensity를 density × scale로 덮어써서,
 * 화면 코드는 디자인 px 숫자를 dp·sp로 그대로 쓴다.
 */
@Composable
fun TraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = TraceColors()
    val type = TraceType()

    // Material3 색상은 최소한만 설정 (배경, 표면)
    val materialColors = darkColorScheme(
        background = colors.panel,
        surface = colors.tile,
        onBackground = colors.t1,
        onSurface = colors.t1,
        primary = colors.accent,
        onPrimary = colors.t1,
    )

    // 칸 크기 측정 후 scale 적용
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthDp = maxWidth.value
        val containerHeightDp = maxHeight.value

        // scale = min(가로비율, 세로비율)
        val scale = min(
            containerWidthDp / DESIGN_WIDTH_DP,
            containerHeightDp / DESIGN_HEIGHT_DP
        )

        val originalDensity = LocalDensity.current
        // density × scale, fontScale은 시스템 설정 유지
        val scaledDensity = Density(
            density = originalDensity.density * scale,
            fontScale = originalDensity.fontScale
        )

        val dimens = TraceDimens(scale = scale)

        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
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
