package com.tracers.trace.ui.screens.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.theme.TraceTheme
import java.time.format.DateTimeFormatter

private val YM_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yy.MM")
private val D_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd")

/**
 * 정비 기록 한 줄: 날짜 칸(폭 40, 일 Inter 16 600 / 연월 Inter 11 t3), 제목 14 600, 보조 줄 12 t2, 금액 14 t2.
 * 디자인 항목 펼침 목록(it.recs)의 행 모양.
 */
@Composable
internal fun MaintenanceRecordRow(
    record: MaintenanceRecord,
    showCost: Boolean,
    contentPadding: PaddingValues,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Row(
        modifier = Modifier.fillMaxWidth().padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(record.date.format(D_FORMAT), style = type.number(16f, FontWeight.SemiBold), color = colors.t1)
            Text(record.date.format(YM_FORMAT), style = type.number(11f, FontWeight.Normal), color = colors.t3)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(record.title, style = type.body(14f, FontWeight.SemiBold), color = colors.t1)
            Text(recordSubline(record, colors), style = type.body(12f), color = colors.t2)
        }
        if (showCost) {
            record.costWon?.let { Text(formatWon(it), style = type.body(14f), color = colors.t2) }
        }
    }
}

/**
 * 정비 기록 목록 (S04-6). 최근 순. 타일 바탕, 모서리 20.
 * 디자인 마크업엔 따로 된 목록이 없어(스크립트의 records·recTitle만 있음) 펼침 목록 행 모양을 썼다.
 */
@Composable
fun MaintenanceRecordList(records: List<MaintenanceRecord>, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        if (records.isEmpty()) {
            Text(
                text = "아직 정비 기록이 없어요",
                style = TraceTheme.type.body(14f),
                color = colors.t2,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            return@Column
        }
        records.sortedByDescending { it.date }.forEachIndexed { i, rec ->
            if (i > 0) RowDivider()
            MaintenanceRecordRow(rec, showCost = true, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp))
        }
    }
}
