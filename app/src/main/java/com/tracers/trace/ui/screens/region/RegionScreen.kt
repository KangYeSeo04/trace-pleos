package com.tracers.trace.ui.screens.region

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.ui.components.DetailHeader
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 지역 상세 화면 (S08, RegionDark.dc.html 지도 보기).
 * 순서: 헤더 → 별자리 지도 → 선택 지역 카드 → 안내 → 범례 → 곧 만나요 → 위치 기록.
 */
@Composable
fun RegionScreen(
    uiState: RegionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectRegion: (String) -> Unit = {},
    onToggleLocationRecording: (Boolean) -> Unit = {},
    onOpenRegionClear: () -> Unit = {},
    onClearAll: () -> Unit = {},
    onOpenDex: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    zoom: MapZoomState = remember { MapZoomState() },
) {
    val colors = TraceTheme.colors
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    // 지도 JSON 파싱·Path 생성은 한 번만, 메인 스레드 밖에서 (미리보기만 바로 읽음)
    val map by produceState(
        initialValue = if (inspection) RegionMapStore.loadBlockingForPreview(context) else null
    ) {
        if (value == null) value = RegionMapStore.load(context)
    }
    val palette = colors.regionPalette
    val regionColors = remember(uiState.visited, palette) {
        uiState.visited.associate { it.code to palette[it.colorIndex.mod(palette.size)] }
    }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.panel)
        ) {
            DetailHeader(
                title = "지역",
                onBack = onNavigateBack,
                actions = { DexButton(onClick = onOpenDex) }
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp)
            ) {
                RegionMapCard(
                    map = map,
                    regionColors = regionColors,
                    selectedCode = uiState.selected?.code,
                    currentCode = uiState.currentRegionCode,
                    visitedCount = uiState.visitedCount,
                    zoom = zoom,
                    onSelect = onSelectRegion,
                )
                Spacer(Modifier.height(10.dp))
                SelectedRegionCard(
                    region = uiState.selected,
                    dotColor = uiState.selected?.let { regionColors[it.code] },
                )
                Text(
                    text = "색이 칠해진 지역을 누르면 달린 거리와 머문 시간을 볼 수 있어요",
                    style = TraceTheme.type.body(12f),
                    color = colors.t3,
                    modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp)
                )
                map?.let { Legend(it) }

                SectionTitle("곧 만나요")
                ComingSoonCard()

                SectionTitle("위치 기록")
                LocationRecordCard(
                    recordingOn = uiState.locationRecordingOn,
                    onToggle = onToggleLocationRecording,
                    onOpenRegionClear = onOpenRegionClear,
                    onClearAll = { confirmClear = true },
                )
            }
        }

        if (confirmClear) {
            ClearAllDialog(
                onCancel = { confirmClear = false },
                onConfirm = {
                    confirmClear = false
                    onClearAll()
                }
            )
        }
    }
}

/** 헤더 오른쪽 [도감 보기] (높이 40, 좌우 16, 모서리 20, 14sp 600) */
@Composable
private fun DexButton(onClick: () -> Unit) {
    val colors = TraceTheme.colors
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "도감 보기", style = TraceTheme.type.body(14f, FontWeight.SemiBold), color = colors.t1)
    }
}

/** 선택 지역 카드 (S08 ⑤): 이름·시·도·색 점 / 달린 거리 / 머문 시간 */
@Composable
private fun SelectedRegionCard(region: VisitedRegion?, dotColor: Color?) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = region?.name ?: "아직 다녀온 지역이 없어요",
                    style = type.body(18f, FontWeight.Bold),
                    color = colors.t1,
                    modifier = Modifier.alignByBaseline()
                )
                if (region != null) {
                    Text(
                        text = region.sidoName,
                        style = type.body(14f),
                        color = colors.t2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
            if (dotColor != null) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            }
        }
        StatRow(
            label = "달린 거리",
            value = region?.let { RegionFormat.distance(it.distanceKm) } ?: "–",
            numeric = true
        )
        StatRow(label = "머문 시간", value = region?.let { RegionFormat.stay(it.stayMinutes) } ?: "–", numeric = false)
    }
}

@Composable
private fun StatRow(label: String, value: String, numeric: Boolean) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = type.body(15f), color = colors.t2)
        Text(
            text = value,
            // 디자인: 달린 거리 Inter 600, 머문 시간 Pretendard 600
            style = if (numeric) type.number(15f, FontWeight.SemiBold) else type.body(15f, FontWeight.SemiBold),
            color = colors.t1
        )
    }
}

/**
 * 범례 (S08 ⑥): 시·도 묶음 색 + 아직 안 감.
 * 디자인 HTML은 legend 값을 계산만 하고 그리지 않는다. 기획안 S08 ⑥을 따라 안내 문구 아래에 둔다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(map: RegionMap) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    val names = map.groups.associate { it.id to it.name }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LegendOrder.forEach { id ->
            LegendItem(names[id] ?: return@forEach) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(colors.regionGroup.getValue(id)))
            }
        }
        LegendItem("아직 안 감") {
            Canvas(Modifier.size(10.dp)) {
                drawCircle(colors.mapLand)
                drawCircle(
                    colors.t3,
                    radius = size.minDimension / 2 - 0.5.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 1.5.dp.toPx())))
                )
            }
        }
    }
}

/** 디자인 legend 순서: 경기 강원 충북 충남 경북 경남 전북 전남 제주 */
private val LegendOrder = listOf("gg", "gw", "cb", "cn", "gb", "gn", "jb", "jn", "jj")

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        swatch()
        Text(text = label, style = TraceTheme.type.body(12f), color = TraceTheme.colors.t2)
    }
}

/** 곧 만나요 (S08 ⑦): 흐림, 누를 수 없음 */
@Composable
private fun ComingSoonCard() {
    val colors = TraceTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        ComingSoonRow("관광지 스탬프", "2시간 머물면 지역 쿠폰")
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        ComingSoonRow("충전하며 쿠폰", "충전 중 근처 가게")
    }
}

@Composable
private fun ComingSoonRow(title: String, sub: String) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = type.body(15f, FontWeight.SemiBold), color = colors.t2)
            Text(text = sub, style = type.body(13f), color = colors.t3)
        }
        Text(text = "준비 중", style = type.body(13f, FontWeight.SemiBold), color = colors.t3)
    }
}

/** 위치 기록 (S08 ⑧): 켜기·끄기 / 지역별로 지우기 / 전체 기록 지우기 */
@Composable
private fun LocationRecordCard(
    recordingOn: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenRegionClear: () -> Unit,
    onClearAll: () -> Unit,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .toggleable(value = recordingOn, role = Role.Switch, onValueChange = onToggle)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "위치 기록하기", style = type.body(15f, FontWeight.SemiBold), color = colors.t1)
                Text(text = "기록은 이 차 안에만 저장돼요", style = type.body(13f), color = colors.t2)
            }
            RecordSwitch(on = recordingOn)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(role = Role.Button, onClick = onOpenRegionClear)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "지역별로 지우기",
                style = type.body(15f, FontWeight.SemiBold),
                color = colors.t1,
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = TraceIcons.ChevronRight, contentDescription = null, tint = colors.t3, modifier = Modifier.size(16.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable(role = Role.Button, onClick = onClearAll)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(text = "전체 기록 지우기", style = type.body(15f, FontWeight.SemiBold), color = colors.warn)
        }
    }
}

/** 스위치 48 × 28, 손잡이 22 (켜짐: accent 바탕·오른쪽 3). 꺼짐 모양은 디자인에 없어 track 바탕·왼쪽 3 */
@Composable
private fun RecordSwitch(on: Boolean) {
    val colors = TraceTheme.colors
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) colors.accent else colors.track),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.switchKnob)
        )
    }
}

/** 전체 지우기 확인 (기획안: "되돌릴 수 없어요" 확인 후 삭제). 디자인에 없어 코인 안내 창 모양을 따른다 */
@Composable
private fun ClearAllDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    BackHandler(onBack = onCancel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dialogScrim)
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
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)
                .semantics { paneTitle = "전체 기록 지우기" }
        ) {
            Text(text = "위치 기록을 모두 지울까요?", style = type.body(19f, FontWeight.Bold), color = colors.t1)
            Text(
                text = "되돌릴 수 없어요. 다녀온 지역, 달린 거리, 머문 시간이 모두 지워져요.",
                style = type.body(15f).copy(lineHeight = 1.5.em),
                color = colors.t2,
                modifier = Modifier.padding(top = 10.dp)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("취소", colors.tile, colors.t1, onCancel, Modifier.weight(1f))
                DialogButton("모두 지우기", colors.tile, colors.warn, onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, bg: Color, fg: Color, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = TraceTheme.type.body(16f, FontWeight.SemiBold), color = fg, textAlign = TextAlign.Center)
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

@Preview(widthDp = 600, heightDp = 1400, showBackground = true)
@Composable
private fun RegionScreenPreview() {
    TraceTheme { RegionScreen(uiState = SampleData.regionUiState, onNavigateBack = {}) }
}

@Preview(widthDp = 600, heightDp = 800, showBackground = true)
@Composable
private fun RegionScreenEmptyPreview() {
    TraceTheme { RegionScreen(uiState = SampleData.regionUiStateEmpty, onNavigateBack = {}) }
}
