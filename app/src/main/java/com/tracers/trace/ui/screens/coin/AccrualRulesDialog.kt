package com.tracers.trace.ui.screens.coin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.tracers.trace.domain.DrivingThresholds
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/** 적립 기준 표 (CoinDark rules). 수치는 판정 상수에서 가져온다. */
internal val accrualRules: List<Pair<String, String>> = listOf(
    "시작 점수" to "85점",
    "${fmt(SafetyScoreChart.ACCRUAL_THRESHOLD.toDouble())}점 이상" to "깨끗한 1 km마다 1코인",
    "${fmt(SafetyScoreChart.ACCRUAL_THRESHOLD.toDouble())}점 미만" to "다시 오를 때까지 멈춤",
    "점수 갱신" to "기존 × 0.98 + 이번 1 km × 0.02",
    "급가속" to "초당 ${fmt(DrivingThresholds.HARD_ACCEL_KMH_PER_SEC)} km/h 이상",
    "급감속" to "초당 ${fmt(DrivingThresholds.HARD_DECEL_KMH_PER_SEC)} km/h 이상",
    // TODO(E-01): 과속 기준(제한속도 + 20)은 판정 상수가 생기면 그 값으로
    "과속" to "제한속도 + 20 km/h 초과",
)

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

/**
 * "적립 기준" 안내 창. 디자인: 뒤 rgba(0,0,0,0.55), 바깥 여백 24, 창 최대 폭 520·모서리 24·안쪽 22 20 20.
 * 뒤로 키·닫기·확인으로 닫힌다.
 */
@Composable
internal fun AccrualRulesDialog(onClose: () -> Unit) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dialogScrim)
            // 뒤 화면 터치 막기 (디자인은 뒤를 눌러도 닫히지 않는다)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(colors.panel)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)
                .semantics { paneTitle = "적립 기준" }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "적립 기준", style = type.body(19f, FontWeight.Bold), color = colors.t1)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.tile)
                        .clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TraceIcons.Close,
                        contentDescription = "닫기",
                        tint = colors.t1,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.tile)
            ) {
                accrualRules.forEachIndexed { i, (k, v) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = k, style = type.body(14f), color = colors.t2)
                        Text(
                            text = v,
                            style = type.body(14f, FontWeight.SemiBold),
                            color = colors.t1,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Text(
                text = "안전점수는 운전 습관을 돌아보는 지표예요. 사고 위험을 판정하지 않고, 차 밖으로 보내지 않아요.",
                style = type.body(12f).copy(lineHeight = 1.6.em),
                color = colors.t3,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp)
            )

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "확인", style = type.body(16f, FontWeight.SemiBold), color = colors.onAccent)
            }
        }
    }
}
