package com.tracers.trace.ui.screens.region

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.ui.components.DetailHeader
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 지역별로 지우기 (S08 ⑧ → 방문 지역 목록, G-04). 디자인에 없는 화면이라 지역 화면의 카드·글자 규칙을 따른다.
 * 한 줄 = 지역 이름, 시·도 · 달린 거리 · 머문 시간, 오른쪽 [지우기]. 지우면 그 지역 기록만 사라진다.
 */
@Composable
fun RegionClearScreen(
    visited: List<VisitedRegion>,
    onNavigateBack: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panel)
    ) {
        DetailHeader(title = "지역별로 지우기", onBack = onNavigateBack)
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        if (visited.isEmpty()) {
            Text(
                text = "지울 지역 기록이 없어요",
                style = type.body(15f),
                color = colors.t2,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
            )
            return@Column
        }
        Text(
            text = "지운 지역은 지도에서 다시 점선으로 바뀌어요",
            style = type.body(13f),
            color = colors.t3,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 14.dp)
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visited, key = { it.code }) { r -> ClearRow(r, onDelete = { onDelete(r.code) }) }
        }
    }
}

@Composable
private fun ClearRow(region: VisitedRegion, onDelete: () -> Unit) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = region.name, style = type.body(15f, FontWeight.SemiBold), color = colors.t1)
            Text(
                text = "${region.sidoName} · ${RegionFormat.distance(region.distanceKm)} · ${RegionFormat.stay(region.stayMinutes)}",
                style = type.body(13f),
                color = colors.t2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onDelete)
                .semantics { contentDescription = "${region.name} 기록 지우기" }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "지우기", style = type.body(15f, FontWeight.SemiBold), color = colors.warn)
        }
    }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun RegionClearScreenPreview() {
    TraceTheme { RegionClearScreen(SampleData.regionUiState.visited, onNavigateBack = {}, onDelete = {}) }
}
