package com.tracers.trace.ui.screens.coin

import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 이번 주행 1km 기록 (E-03). 디자인(CoinDark·CoinLight): 타일 모서리 20, 안쪽 20 22.
 * 가로 막대 하나(높이 12, 모서리 6)에 적립 구간은 금색, 멈춘 구간은 회색(dim).
 * 이어진 멈춤 km는 한 구간으로 묶어 아래에 "7" 또는 "12–14"로 표시한다.
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
        if (record == null || record.totalKm == 0) {
            Text(text = "아직 주행 기록이 없어요", style = type.body(17f, FontWeight.Bold), color = colors.t1)
            Spacer(Modifier.height(6.dp))
            Text(text = "주행을 마치면 1 km마다 적립 기록이 생겨요", style = type.body(14f), color = colors.t2)
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.alignByBaseline()
            ) {
                LegendItem("적립", colors.gold)
                LegendItem("멈춤", colors.dim)
            }
        }

        val runs = record.stoppedRuns
        val total = record.totalKm.toFloat()
        Spacer(Modifier.height(18.dp))
        // 가로 막대: 바탕 금색, 멈춘 구간만 회색 (최소 폭 4)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.gold)
                .semantics { contentDescription = stripDescription(record.totalKm, runs) }
        ) {
            runs.forEach { run ->
                Box(
                    modifier = Modifier
                        .offset(x = maxWidth * ((run.first - 1) / total))
                        .width(maxOf(maxWidth * ((run.last - run.first + 1) / total), 4.dp))
                        .fillMaxHeight()
                        .background(colors.dim)
                )
            }
        }

        // 아래 눈금: 왼쪽 0, 멈춘 구간 가운데에 km, 오른쪽 전체 km
        Spacer(Modifier.height(6.dp))
        Layout(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            content = {
                Text("0", style = type.number(12f, FontWeight.Normal), color = colors.t3)
                runs.forEach { run ->
                    Text(
                        text = if (run.first == run.last) "${run.first}" else "${run.first}–${run.last}",
                        style = type.number(12f, FontWeight.Normal),
                        color = colors.t2,
                        maxLines = 1,
                    )
                }
                Text("${record.totalKm} km", style = type.number(12f, FontWeight.Normal), color = colors.t3)
            }
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measurables.map { it.measure(loose) }
            val w = constraints.maxWidth
            layout(w, constraints.maxHeight) {
                placeables.first().place(0, 0)
                placeables.last().place(w - placeables.last().width, 0)
                runs.forEachIndexed { i, run ->
                    val p = placeables[i + 1]
                    val mid = ((run.first - 1) + (run.last - run.first + 1) / 2f) / total
                    // translateX(-50%), 칸 밖으로 나가지 않게만 막는다
                    val x = (w * mid - p.width / 2f).toInt().coerceIn(0, (w - p.width).coerceAtLeast(0))
                    p.place(x, 0)
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

/** 범례 한 칸: 점 8 + 간격 5 + 글자 13 */
@Composable
private fun LegendItem(label: String, dot: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(text = label, style = TraceTheme.type.body(13f), color = TraceTheme.colors.t2)
    }
}

/** 화면 읽기용: "37 km 중 7 km, 12–14 km에서 적립이 멈췄어요" (CoinDark stripAria) */
internal fun stripDescription(totalKm: Int, runs: List<IntRange>): String =
    if (runs.isEmpty()) "$totalKm km 모두 적립했어요"
    else "$totalKm km 중 " + runs.joinToString(", ") {
        if (it.first == it.last) "${it.first} km" else "${it.first}–${it.last} km"
    } + "에서 적립이 멈췄어요"

@Composable
private fun EventCount(label: String, count: Int, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text = label, style = type.body(13f), color = colors.t2)
        Text(text = count.toString(), style = type.number(18f), color = colors.t1)
    }
}
