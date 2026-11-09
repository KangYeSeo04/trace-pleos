package com.tracers.trace.ui.screens.coin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/** 한 줄 칸 수 (디자인 grid repeat(10)) */
private const val CELLS_PER_ROW = 10

/**
 * 이번 주행 1km 기록 (E-03). 디자인: 타일 모서리 20, 안쪽 20 22, 칸 높이 34·모서리 8·간격 6.
 * 깨끗한 km는 금색 별(goldSoft 바탕), 위험 km는 흐린 별(cellOff 바탕).
 */
@Composable
internal fun TripKmRecordCard(record: TripKmRecord?, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        if (record == null) {
            Text(text = "아직 주행 기록이 없어요", style = type.body(17f, FontWeight.Bold), color = colors.t1)
            Spacer(Modifier.height(6.dp))
            Text(text = "주행을 마치면 1 km마다 별 한 칸이 생겨요", style = type.body(14f), color = colors.t2)
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${record.totalKm} km 중 ${record.cleanKm} km 깨끗하게",
                style = type.body(17f, FontWeight.Bold),
                color = colors.t1,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = "1칸 = 1 km",
                style = type.body(13f),
                color = colors.t2,
                modifier = Modifier.alignByBaseline()
            )
        }

        Spacer(Modifier.height(16.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics {
                contentDescription = "${record.totalKm} km 중 ${record.cleanKm} km 깨끗하게"
            }
        ) {
            record.cleanCells.chunked(CELLS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until CELLS_PER_ROW) {
                        val clean = row.getOrNull(i)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .then(
                                    if (clean == null) Modifier
                                    else Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (clean) colors.goldSoft else colors.cellOff)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (clean != null) {
                                Icon(
                                    imageVector = TraceIcons.Star4,
                                    contentDescription = null,
                                    tint = if (clean) colors.gold else colors.dim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 이벤트 4종: 위 18, 구분선, 안쪽 위 14
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            EventCount("급가속", record.hardAccel, Modifier.weight(1f))
            EventCount("급감속", record.hardBrake, Modifier.weight(1f))
            EventCount("급정거", record.hardStop, Modifier.weight(1f))
            EventCount("과속", record.speeding, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EventCount(label: String, count: Int, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = label, style = type.body(13f), color = colors.t2)
        Text(text = count.toString(), style = type.number(18f), color = colors.t1)
    }
}
