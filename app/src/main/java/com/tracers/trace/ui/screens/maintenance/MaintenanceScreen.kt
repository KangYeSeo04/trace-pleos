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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
 * @param onRecordVisit 방문 감지 [기록하기] → S05 (2회차)
 * @param onDismissVisit 방문 감지 [정비 안 했어요] → 카드 숨김
 */
@Composable
fun MaintenanceScreen(
    uiState: MaintenanceUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRecordVisit: () -> Unit = {},
    onDismissVisit: () -> Unit = {},
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
            uiState.visit?.let { visit ->
                VisitDetectionCard(visit = visit, onRecord = onRecordVisit, onDismiss = onDismissVisit)
            }

            // 방문 감지 카드가 없으면 첫 섹션 제목이 스크롤 영역 맨 위에 붙는다
            SectionTitle("점검 항목", top = if (uiState.visit == null) 0.dp else 26.dp)
            MaintenanceItemsCard(items = uiState.items)

            // 정비 기록: 디자인엔 따로 된 목록이 없다. 항목 행을 펼치면 그 항목 기록이,
            // 달력에서 날을 고르면 그날 기록이 보인다 (uiState.records)

            SectionTitle("정비 달력")
            // TODO(2회차): 정비 달력 카드 (uiState.calendar, uiState.records)

            SectionTitle("기록 추가")
            // TODO(2회차): 기록 추가 양식 S05 (uiState.form)
        }
    }
}

/** 섹션 제목: 위 26, 좌우 4, 아래 10, 15sp 600 t2 */
@Composable
private fun SectionTitle(text: String, top: Dp = 26.dp) {
    Text(
        text = text,
        style = TraceTheme.type.body(15f, FontWeight.SemiBold),
        color = TraceTheme.colors.t2,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = top, bottom = 10.dp)
    )
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
