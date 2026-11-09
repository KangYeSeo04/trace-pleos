package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.ui.components.ProgressBar
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceColors
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 점검 항목 카드 (S04-3·4, D-01). 타일 바탕, 모서리 20, 행 사이 1px 구분선.
 * 행을 누르면 그 항목의 정비 기록이 아래에 펼쳐지고, 다시 누르면 접힌다 (디자인 it.open).
 *
 * @param records 펼침 목록용 정비 기록. 항목 이름이 itemNames에 있는 기록만 보인다
 */
@Composable
fun MaintenanceItemsCard(
    items: List<MaintenanceItem>,
    records: List<MaintenanceRecord>,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    var openName by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        if (items.isEmpty()) {
            Text(
                text = "아직 점검 항목 정보가 없어요",
                style = TraceTheme.type.body(14f),
                color = colors.t2,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            return@Column
        }
        items.forEachIndexed { i, item ->
            if (i > 0) {
                RowDivider()
            }
            MaintenanceItemRow(
                item = item,
                onClick = { openName = if (openName == item.name) null else item.name }
            )
            if (openName == item.name) {
                ItemRecords(records.filter { item.name in it.itemNames })
            }
        }
    }
}

/** 펼침 목록: 좌우 20·아래 14 띄움, 패널 바탕, 1px 테두리, 모서리 14 */
@Composable
private fun ItemRecords(records: List<MaintenanceRecord>) {
    val colors = TraceTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(1.dp, colors.line, shape)
    ) {
        records.forEachIndexed { i, rec ->
            if (i > 0) RowDivider()
            MaintenanceRecordRow(rec, showCost = false, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp))
        }
        if (records.isEmpty()) {
            Text(
                text = "아직 정비 기록이 없어요",
                style = TraceTheme.type.body(14f),
                color = colors.t2,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

/** 항목 행: 안쪽 16/20, 최소 높이 76, 이름 줄과 막대 줄 사이 10 */
@Composable
private fun MaintenanceItemRow(item: MaintenanceItem, onClick: () -> Unit) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    val itemColor = levelColor(item.level, colors)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(item.name, item.remaining, item.basisNote, "눌러서 정비 기록 보기").joinToString(", ")
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                style = type.body(16f, FontWeight.SemiBold),
                color = colors.t1,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.remaining,
                    style = type.body(15f, FontWeight.SemiBold),
                    // 75% 이상이면 막대 색, 그 외 t2
                    color = if (item.level == UsageLevel.Normal) colors.t2 else itemColor
                )
                Icon(
                    imageVector = TraceIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.t3,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        // 두 기준(거리·기간)이면 막대 2줄
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item.gauges.forEach { gauge -> GaugeRow(gauge) }
            // 기록이 없어 처음 본 값부터 센 항목. 디자인에 없는 줄이라 기준 값과 같은 12sp, 흐린 글자
            item.basisNote?.let {
                Text(text = it, style = TraceTheme.type.body(12f), color = colors.t3)
            }
        }
    }
}

/** 막대 줄: 막대(높이 4) + 기준 값(Inter 12 500 t2, 최소 폭 128 오른쪽 정렬), 사이 12 */
@Composable
private fun GaugeRow(gauge: UsageGauge) {
    val colors = TraceTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressBar(
            percent = gauge.percent,
            barColor = levelColor(gauge.level, colors),
            trackColor = colors.itemTrack,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = gauge.basis,
            style = TraceTheme.type.number(12f, FontWeight.Medium).copy(letterSpacing = (-0.12).sp),
            color = colors.t2,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 128.dp)
        )
    }
}

/** 90% 이상 경고, 75% 이상 주의, 그 외 회색 */
private fun levelColor(level: UsageLevel, colors: TraceColors): Color = when (level) {
    UsageLevel.Warn -> colors.warn
    UsageLevel.Caution -> colors.caution
    UsageLevel.Normal -> colors.barNeutral
}
