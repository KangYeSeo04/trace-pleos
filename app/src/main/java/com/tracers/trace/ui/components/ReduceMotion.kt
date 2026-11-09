package com.tracers.trace.ui.components

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 모션 줄이기 상태에서 움직임 속도 배율 (디자인의 prefers-reduced-motion 처리: 0.2배) */
const val REDUCED_MOTION_SPEED = 0.2f

/**
 * 접근성 "애니메이션 삭제"(모션 줄이기)가 켜져 있는지.
 * Android에서는 이 설정이 ANIMATOR_DURATION_SCALE = 0 으로 나타난다.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
