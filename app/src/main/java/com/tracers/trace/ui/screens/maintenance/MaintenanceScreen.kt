package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.ui.components.DetailHeader
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 정비 상세 화면 (S04). 디자인: design/html/MaintDark.dc.html (600 × 2600)
 *
 * @param onNavigateBack 뒤로 → 홈
 */
@Composable
fun MaintenanceScreen(
    uiState: MaintenanceUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val colors = TraceTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel)
    ) {
        // 헤더 (sticky): 높이 52 + 아래 1px 구분선
        DetailHeader(title = "정비", onBack = onNavigateBack)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line)
        )

        // 스크롤 영역: 좌우 20, 위 12, 아래 28
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp)
        ) {
        }
    }
}

// ===== Preview =====

@Preview(widthDp = 600, heightDp = 2600, showBackground = true)
@Composable
private fun MaintenanceScreenPreview() {
    TraceTheme { MaintenanceScreen(uiState = SampleData.maintenanceUiState, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun MaintenanceScreenTwoGaugesPreview() {
    TraceTheme { MaintenanceScreen(uiState = SampleData.maintenanceUiStateTwoGauges, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun MaintenanceScreenEmptyPreview() {
    TraceTheme { MaintenanceScreen(uiState = SampleData.maintenanceUiStateEmpty, onNavigateBack = {}) }
}
