package com.tracers.trace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tracers.trace.ui.theme.TraceTheme

/**
 * S01(정차), S02(주행) 홈 화면.
 * 1단계에서 디자인 구현 예정.
 */
@Composable
fun HomeScreen(
    onNavigateToEfficiency: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToCoin: () -> Unit = {},
    onNavigateToRegion: () -> Unit = {},
    onNavigateToSpace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 0단계: 다크 패널 색으로 빈 화면
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TraceTheme.colors.panel)
    )
}

@Preview(widthDp = 600, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    TraceTheme {
        HomeScreen()
    }
}
