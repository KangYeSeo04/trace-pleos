package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 *
 * @param onItemClick 행 탭 → 그 항목 정비 기록 펼치기 (2회차)
 */
@Composable
fun MaintenanceItemsCard(
    items: List<MaintenanceItem>,
    modifier: Modifier = Modifier,
    onItemClick: (MaintenanceItem) -> Unit = {},
) {
    val colors = TraceTheme.colors

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
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
            }
            // TODO(2회차): 펼치면 이 항목의 정비 기록 목록 (디자인 it.open / it.recs)
            MaintenanceItemRow(item = item, onClick = { onItemClick(item) })
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
                contentDescription = "${item.name}, ${item.remaining}, 눌러서 정비 기록 보기"
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
