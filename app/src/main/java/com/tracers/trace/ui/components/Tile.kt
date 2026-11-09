package com.tracers.trace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import com.tracers.trace.ui.screens.region.RegionMapStore
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.tracers.trace.ui.theme.InterFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.screens.home.StatusLevel
import com.tracers.trace.ui.screens.home.TileState
import com.tracers.trace.ui.theme.TraceTheme

/**
 * 홈 화면 타일 컴포넌트.
 *
 * @param state 타일 상태
 * @param isDriving 주행 모드 여부
 * @param onClick 클릭 콜백
 */
@Composable
fun Tile(
    state: TileState,
    isDriving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    val (icon, label) = when (state) {
        is TileState.Efficiency -> TraceIcons.Efficiency to "전비"
        is TileState.Maintenance -> TraceIcons.Maintenance to "정비"
        is TileState.Coin -> TraceIcons.Coin to "코인"
        is TileState.Region -> TraceIcons.Region to "지역"
    }

    val statusColor = when (state.statusLevel) {
        StatusLevel.Normal -> colors.t2
        StatusLevel.Good -> colors.good
        StatusLevel.Caution -> colors.caution
        StatusLevel.Warn -> colors.warn
        StatusLevel.Gold -> colors.gold
    }

    val barColor = when (state) {
        is TileState.Efficiency -> colors.t1
        is TileState.Maintenance -> colors.warn
        is TileState.Coin -> colors.gold
        is TileState.Region -> colors.accent
    }

    // 주행 모드에 따른 글자 크기
    val bigSize = when (state) {
        is TileState.Region -> if (isDriving) 40.sp else 34.sp
        else -> if (isDriving) 52.sp else 44.sp
    }

    // TODO(A-04): 주행 모드에서 음성 재생 중에만 파란 테두리 + "안내 중" 표시
    val borderModifier = Modifier

    val density = androidx.compose.ui.platform.LocalDensity.current
    val tileLabel = when (state) {
        is TileState.Efficiency -> "Efficiency"
        is TileState.Maintenance -> "Maintenance"
        is TileState.Coin -> "Coin"
        is TileState.Region -> "Region"
    }

    // 1초 크로스페이드는 타일 내용에만 적용
    Crossfade(
        targetState = isDriving,
        animationSpec = tween(durationMillis = 1000),
        label = "TileContent_$tileLabel",
        modifier = modifier.fillMaxSize()
    ) { driving: Boolean ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.tile)
                .then(borderModifier)
                .clickable(enabled = !driving, onClick = onClick)
                .padding(18.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // 헤더
            TileHeader(
                icon = icon,
                label = label,
                isDriving = driving
        )

        Spacer(modifier = Modifier.weight(1f))

        // 값 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            if (state is TileState.Region) {
                // 지역 타일: 도시 + 구 + 지도
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = state.city,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = bigSize,
                                letterSpacing = bigSize * -0.035f,
                                fontFeatureSettings = "tnum"
                            ),
                            color = colors.t1
                        )
                        Text(
                            text = state.district,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.t2
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                // 지금 지역 작은 지도 (정차 모드)
                val mapCode = state.mapCode
                if (!isDriving && mapCode != null) {
                    RegionMiniMap(
                        code = mapCode,
                        latitude = state.mapLatitude,
                        longitude = state.mapLongitude,
                        modifier = Modifier.size(width = 78.dp, height = 62.dp)
                    )
                }
            } else {
                // 일반 타일: 값 + 단위
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = state.value,
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = bigSize,
                            letterSpacing = bigSize * -0.035f,
                            fontFeatureSettings = "tnum"
                        ),
                        color = colors.t1
                    )
                    if (state.unit.isNotEmpty()) {
                        Text(
                            text = state.unit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.t2
                        )
                    }
                }
            }
        }

        // 상태 텍스트
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.status,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor
        )

        // 진행 막대 (주행 모드에서는 보이지 않지만 자리는 유지)
        Spacer(modifier = Modifier.height(14.dp))
        TileProgressBar(
            percent = state.barPercent,
            barColor = barColor,
            markPercent = state.barMarkPercent,
            modifier = Modifier.alpha(if (driving) 0f else 1f)
        )

        // 보조 텍스트 (주행 모드에서는 보이지 않지만 자리는 유지)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = state.subText,
            fontSize = 13.sp,
            color = colors.t2,
            modifier = Modifier.alpha(if (driving) 0f else 1f)
        )
        }
    }
}

@Composable
private fun TileHeader(
    icon: ImageVector,
    label: String,
    isDriving: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.t2,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.t2
            )
        }

        // 오른쪽 아이콘: 주행 모드면 스피커, 정차 모드면 셰브런
        if (isDriving) {
            Icon(
                imageVector = TraceIcons.Speaker,
                contentDescription = "음성 안내",
                tint = colors.t3,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = TraceIcons.ChevronRight,
                contentDescription = "자세히 보기",
                tint = colors.t3,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TileProgressBar(
    percent: Float,
    barColor: Color,
    markPercent: Float?,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        // 배경
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.track)
        )

        // 채움
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

        // 눈금 (markPercent가 있을 때)
        if (markPercent != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val x = size.width * (markPercent / 100f)
                drawLine(
                    color = colors.t2,
                    start = Offset(x, -3.dp.toPx()),
                    end = Offset(x, size.height + 3.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 지금 지역의 모양(regions_display.json)과 지금 위치 점. 칸에 맞춰 가운데 놓고, 선 굵기·점 크기는 칸 기준으로 고정.
 * 지도 데이터는 지역 화면과 같은 것을 한 번만 읽어 같이 쓴다.
 */
@Composable
private fun RegionMiniMap(
    code: String,
    latitude: Double?,
    longitude: Double?,
    modifier: Modifier = Modifier
) {
    val colors = TraceTheme.colors
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val map by produceState(
        initialValue = if (inspection) RegionMapStore.loadBlockingForPreview(context) else null
    ) {
        if (value == null) value = RegionMapStore.load(context)
    }
    val mini = remember(map, code, latitude, longitude) { map?.miniMap(code, latitude, longitude) } ?: return

    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val dotR = 9f * size.width / 100f
        // 점이 모양 밖(해안 근처)이어도 보이게 범위에 넣고, 선·점이 잘리지 않게 여백
        val b = mini.dot?.let { d ->
            Rect(minOf(mini.bounds.left, d.x), minOf(mini.bounds.top, d.y), maxOf(mini.bounds.right, d.x), maxOf(mini.bounds.bottom, d.y))
        } ?: mini.bounds
        val pad = maxOf(stroke, dotR)
        val w = (b.width).coerceAtLeast(1e-3f)
        val h = (b.height).coerceAtLeast(1e-3f)
        val k = minOf((size.width - 2 * pad) / w, (size.height - 2 * pad) / h)
        val ox = (size.width - w * k) / 2 - b.left * k
        val oy = (size.height - h * k) / 2 - b.top * k

        withTransform({
            translate(ox, oy)
            scale(k, k, pivot = Offset.Zero)
        }) {
            drawPath(
                path = mini.path,
                color = colors.t2,
                style = Stroke(width = stroke / k, join = StrokeJoin.Round)
            )
        }

        val d = mini.dot ?: return@Canvas
        val center = Offset(d.x * k + ox, d.y * k + oy)
        // 지금 위치 (큰 원 반투명 + 작은 원)
        drawCircle(color = colors.accent.copy(alpha = 0.22f), radius = dotR, center = center)
        drawCircle(color = colors.accent, radius = 4.6f * size.width / 100f, center = center)
    }
}
