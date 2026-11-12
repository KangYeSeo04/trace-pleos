package com.tracers.trace.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.min

/** 테마 전환 크로스페이드 길이 */
const val THEME_CROSSFADE_MS = 1000

/**
 * TRACE 테마.
 *
 * darkTheme에 따라 색 묶음(DarkTraceColors / LightTraceColors)만 바뀐다.
 * 바뀔 때는 두 묶음을 1초 동안 섞어 크로스페이드한다(공통 규칙: 상태 전환 1초).
 * Material3는 쓰되 기본 테마 색은 쓰지 않는다 (TraceColors 사용).
 *
 * 배치(TraceLayout)는 칸 가로/세로 비율로 고르고(3분할 600×800 / 전체 화면 1200×800),
 * scale = min(칸가로/기준가로, 칸세로/기준세로)로 동적 계산.
 * LocalDensity를 density × scale로 덮어써서,
 * 화면 코드는 디자인 px 숫자를 dp·sp로 그대로 쓴다.
 */
@Composable
fun TraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val toDark by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = THEME_CROSSFADE_MS),
        label = "themeCrossfade",
    )
    val colors = lerpTraceColors(LightTraceColors, DarkTraceColors, toDark)
    val type = TraceType()

    // Material3 색상은 최소한만 설정 (배경, 표면)
    val materialColors = if (darkTheme) {
        darkColorScheme(
            background = colors.panel,
            surface = colors.tile,
            onBackground = colors.t1,
            onSurface = colors.t1,
            primary = colors.accent,
            onPrimary = colors.onAccent,
        )
    } else {
        lightColorScheme(
            background = colors.panel,
            surface = colors.tile,
            onBackground = colors.t1,
            onSurface = colors.t1,
            primary = colors.accent,
            onPrimary = colors.onAccent,
        )
    }

    // 칸 크기 측정 후 scale 적용
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthDp = maxWidth.value
        val containerHeightDp = maxHeight.value

        // 칸 비율로 3분할(600×800) / 전체 화면(1200×800) 기준을 고른다
        val layout = TraceLayout.select(containerWidthDp, containerHeightDp)

        // scale = min(칸 가로 / 기준 가로, 칸 세로 / 기준 세로)
        val scale = min(
            containerWidthDp / layout.designWidth,
            containerHeightDp / layout.designHeight
        )
        LaunchedEffect(containerWidthDp, containerHeightDp) {
            Log.d(
                "TraceLayout",
                "칸 ${containerWidthDp} × ${containerHeightDp} dp → $layout, scale ${"%.3f".format(scale)}"
            )
        }

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
            LocalTraceIsDark provides darkTheme,
            LocalTraceType provides type,
            LocalTraceDimens provides dimens,
            LocalTraceLayout provides layout,
        ) {
            MaterialTheme(
                colorScheme = materialColors,
                content = content
            )
        }
    }
}

/** 지금 테마가 다크인지. 테마별 자산(하늘 사진·비행 영상) 고를 때 쓴다 */
val LocalTraceIsDark = staticCompositionLocalOf { true }

/**
 * 현재 테마 색상에 접근.
 * 사용: TraceTheme.colors.panel
 */
object TraceTheme {
    val colors: TraceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceIsDark.current

    val type: TraceType
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceType.current

    val dimens: TraceDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceDimens.current

    val layout: TraceLayout
        @Composable
        @ReadOnlyComposable
        get() = LocalTraceLayout.current
}
