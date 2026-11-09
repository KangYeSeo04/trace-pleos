package com.tracers.trace.ui.screens.coin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.tracers.trace.ui.components.HeaderIconButton
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 코인 상세 화면 (S06). 디자인: design/html/CoinDark.dc.html (600 × 1900)
 *
 * @param onNavigateBack 뒤로 → 홈
 * @param onClaimCoins "N코인 받기" 버튼. 적립 처리는 2단계(E-04)
 * @param onOpenHelp 헤더 "적립 기준 보기". 안내 창은 2회차
 */
@Composable
fun CoinScreen(
    uiState: CoinUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onClaimCoins: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
) {
    val colors = TraceTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel)
    ) {
        // 헤더 (sticky): 높이 52 + 아래 1px 구분선
        DetailHeader(
            title = "코인",
            onBack = onNavigateBack,
            actions = {
                HeaderIconButton(
                    icon = TraceIcons.Help,
                    contentDescription = "적립 기준 보기",
                    onClick = onOpenHelp
                )
            }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.line)
        )

        // 스크롤 영역: 좌우 20, 아래 28
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

// ===== Preview =====

@Preview(widthDp = 600, heightDp = 1900, showBackground = true)
@Composable
private fun CoinScreenPreview() {
    TraceTheme { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun CoinScreenStoppedPreview() {
    TraceTheme { CoinScreen(uiState = SampleData.coinUiStateStopped, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun CoinScreenEmptyPreview() {
    TraceTheme { CoinScreen(uiState = SampleData.coinUiStateEmpty, onNavigateBack = {}) }
}
