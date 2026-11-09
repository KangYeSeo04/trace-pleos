package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import java.time.LocalDate
import java.time.YearMonth

/**
 * 정비 상세 화면 (S04). 디자인: design/html/MaintDark.dc.html (600 × 2600)
 *
 * @param onNavigateBack 뒤로 → 홈
 * @param onRecordVisit 방문 감지 [기록하기] → S05 (2회차)
 * @param onDismissVisit 방문 감지 [정비 안 했어요] → 카드 숨김
 * @param onPrevMonth 달력 이전 달
 * @param onNextMonth 달력 다음 달
 * @param onSelectDate 달력 날짜 고르기
 * @param onSelectFilter 기록 필터 고르기 (null = 전체)
 * @param formActions 기록 추가 양식 입력
 */
@Composable
fun MaintenanceScreen(
    uiState: MaintenanceUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRecordVisit: () -> Unit = {},
    onDismissVisit: () -> Unit = {},
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectDate: (LocalDate) -> Unit = {},
    onSelectFilter: (String?) -> Unit = {},
    formActions: MaintenanceFormActions = MaintenanceFormActions(),
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
                .imePadding()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp)
        ) {
            uiState.visit?.let { visit ->
                VisitDetectionCard(visit = visit, onRecord = onRecordVisit, onDismiss = onDismissVisit)
            }

            // 방문 감지 카드가 없으면 첫 섹션 제목이 스크롤 영역 맨 위에 붙는다
            SectionTitle("점검 항목", top = if (uiState.visit == null) 0.dp else 26.dp)
            MaintenanceItemsCard(items = uiState.items, records = uiState.records)

            SectionTitle("정비 달력")
            MaintenanceCalendarCard(
                // 미리보기처럼 ViewModel 없이 그릴 때는 이번 달
                calendar = uiState.calendar ?: MaintenanceCalendar(YearMonth.now()),
                records = uiState.records,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate
            )

            SectionTitle("정비 기록")
            if (uiState.records.isNotEmpty()) {
                RecordFilterRow(
                    options = uiState.filterOptions,
                    selected = uiState.recordFilter,
                    onSelect = onSelectFilter,
                )
            }
            MaintenanceRecordList(records = uiState.filteredRecords)

            SectionTitle("기록 추가")
            MaintenanceRecordFormCard(form = uiState.form, actions = formActions)
        }
    }
}

/**
 * 기록 필터 (S04-5): 전체 + 항목별. 디자인에 없어 기록 추가 양식의 항목 칩 모양을 가로 스크롤로 썼다.
 * 칩 사이 8, 목록과 사이 10
 */
@Composable
private fun RecordFilterRow(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ItemChip("전체", selected = selected == null, onClick = { onSelect(null) })
        options.forEach { name -> ItemChip(name, selected = selected == name, onClick = { onSelect(name) }) }
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
