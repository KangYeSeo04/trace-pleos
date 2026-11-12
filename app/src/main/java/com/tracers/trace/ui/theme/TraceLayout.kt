package com.tracers.trace.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * TRACE 칸 배치. 기준 프레임 크기는 여기 한 곳에만 둔다.
 *
 * - [Compact]: 3분할 오른쪽 칸 600 × 800 (Home·Eff·… Dark/Light.dc.html)
 * - [Wide]: 전체 화면(이분할) 1200 × 800 (Full*.dc.html, design/FULL.md)
 */
enum class TraceLayout(val designWidth: Float, val designHeight: Float) {
    Compact(600f, 800f),
    Wide(1200f, 800f);

    /** 칸 가로/세로 비율 */
    val aspect: Float get() = designWidth / designHeight

    companion object {
        /**
         * 칸 가로/세로 비율이 이 값 이상이면 [Wide].
         * 두 기준 비율(0.75, 1.5)의 가운데. 3분할 실측 841 × 1210 dp = 0.70
         */
        val WIDE_ASPECT_THRESHOLD: Float = (Compact.aspect + Wide.aspect) / 2f

        /** 칸 크기(dp)로 배치를 고른다 */
        fun select(widthDp: Float, heightDp: Float): TraceLayout =
            if (heightDp > 0f && widthDp / heightDp >= WIDE_ASPECT_THRESHOLD) Wide else Compact
    }
}

/** 지금 칸의 배치. 화면 코드는 이것으로 3분할/전체 화면 배치를 고른다 */
val LocalTraceLayout = staticCompositionLocalOf { TraceLayout.Compact }
