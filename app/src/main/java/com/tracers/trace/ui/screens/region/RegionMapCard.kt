package com.tracers.trace.ui.screens.region

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.tracers.trace.ui.icons.TraceIcons
import com.tracers.trace.ui.theme.DarkTraceColors
import com.tracers.trace.ui.theme.LightTraceColors
import com.tracers.trace.ui.theme.TraceTheme
import kotlin.math.abs
import kotlin.math.min

/** 지도 칸 높이 (RegionDark: height 608, 모서리 20) */
internal val MapCardHeight = 608.dp

/**
 * 확대·이동 상태. 단위는 지도 칸의 디자인 px(dp). 디자인 Z 객체와 같은 규칙:
 * 화면 = 지도 × s + (x, y), s는 1~5, 지도가 칸 밖으로 비지 않게 x·y를 묶는다.
 */
@Stable
class MapZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set
    internal var width = 560f
    internal var height = 608f

    /** 점 (mx, my)을 고정한 채 배율을 바꾼다 */
    fun zoomAt(mx: Float, my: Float, newScale: Float) {
        val ns = newScale.coerceIn(MIN_SCALE, MAX_SCALE)
        val r = ns / scale
        offsetX = mx - (mx - offsetX) * r
        offsetY = my - (my - offsetY) * r
        scale = ns
        clamp()
    }

    fun panBy(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
        clamp()
    }

    fun zoomIn() = zoomAt(width / 2, height / 2, scale * BUTTON_STEP)
    fun zoomOut() = zoomAt(width / 2, height / 2, scale / BUTTON_STEP)

    fun reset() {
        scale = 1f; offsetX = 0f; offsetY = 0f
    }

    /** 칸 좌표 → 확대 전 칸 좌표 */
    fun toContent(x: Float, y: Float) = Offset((x - offsetX) / scale, (y - offsetY) / scale)

    private fun clamp() {
        scale = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        offsetX = offsetX.coerceIn(min(0f, width - width * scale), 0f)
        offsetY = offsetY.coerceIn(min(0f, height - height * scale), 0f)
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val BUTTON_STEP = 1.5f
    }
}

/**
 * 별자리 지도 칸 (S08 ②③④). 지도 위 제목·원래 크기 버튼·안내 문구는 디자인 그대로, 확대·축소 버튼은 추가.
 *
 * @param map 지도 데이터. 읽는 중이면 null(바다색 칸만 그린다)
 * @param regionColors 색칠할 지역 코드 → 색
 * @param onSelect 색칠된 지역을 탭했을 때
 */
@Composable
internal fun RegionMapCard(
    map: RegionMap?,
    regionColors: Map<String, Color>,
    selectedCode: String?,
    currentCode: String?,
    visitedCount: Int,
    zoom: MapZoomState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MapCardHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.mapSea)
    ) {
        if (map != null) {
            MapCanvas(map, regionColors, selectedCode, currentCode, zoom, onSelect)
        }

        // 왼쪽 위: 대한민국 / N / 161 지역
        Column(
            modifier = Modifier.padding(start = 20.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = "대한민국", style = type.body(22f, FontWeight.Bold), color = colors.mapText)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = type.numberFontFamily, fontWeight = FontWeight.Bold, color = colors.mapText)) {
                        append(visitedCount.toString())
                    }
                    append(" / ${map?.regions?.size ?: "–"} 지역")
                },
                style = type.body(14f),
                color = colors.mapSub
            )
        }

        // 오른쪽 가운데: 확대·축소 (디자인에 없음, 기획안 S08 ③)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapRoundButton(TraceIcons.Plus, "지도 확대", zoom::zoomIn)
            MapRoundButton(TraceIcons.Minus, "지도 축소", zoom::zoomOut)
        }

        // 왼쪽 아래: 원래 크기 + 안내
        MapRoundButton(
            icon = TraceIcons.FitScreen,
            description = "지도 원래 크기로",
            onClick = zoom::reset,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp)
        )
        Text(
            text = "두 손가락으로 확대",
            style = type.body(12f),
            color = colors.mapSub,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 66.dp, bottom = 26.dp)
        )
    }
}

@Composable
private fun MapRoundButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TraceTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.mapSea)
            .border(1.dp, colors.mapLine, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = colors.mapText, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MapCanvas(
    map: RegionMap,
    regionColors: Map<String, Color>,
    selectedCode: String?,
    currentCode: String?,
    zoom: MapZoomState,
    onSelect: (String) -> Unit,
) {
    val colors = TraceTheme.colors
    val type = TraceTheme.type
    val measurer = rememberTextMeasurer()
    // 라벨 글자는 지도마다 한 번만 잰다
    val groupLabels: List<Pair<MapGroup, TextLayoutResult>> = remember(map, measurer) {
        val style = type.body(16f, FontWeight.Bold)
        map.groups.filter { it.labelX != null }.map { it to measurer.measure(it.name, style) }
    }
    val insetLabels: List<Pair<MapInset, TextLayoutResult>> = remember(map, measurer) {
        val style = type.body(12f, FontWeight.SemiBold)
        map.insets.map { it to measurer.measure(it.label, style) }
    }
    val visitedPaths = remember(map, regionColors) {
        regionColors.mapNotNull { (code, c) -> map.byCode[code]?.let { it to c } }
    }
    val selected = selectedCode?.let { map.byCode[it] }
    val current = currentCode?.let { map.byCode[it] }
    val accept by rememberUpdatedState<(String) -> Boolean>({ it in regionColors })
    val select by rememberUpdatedState(onSelect)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        zoom.width = maxWidth.value
        zoom.height = maxHeight.value
        // viewBox(563.3 × 612)를 칸에 맞춰 가운데 (SVG preserveAspectRatio 기본값)
        val k0 = min(maxWidth.value / map.width, maxHeight.value / map.height)
        val ox = (maxWidth.value - map.width * k0) / 2
        val oy = (maxHeight.value - map.height * k0) / 2
        val dpx = LocalDensity.current.density
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        // 테마 전환 1초 동안 colors는 매 프레임 섞인 값이라, 비트맵은 바뀔 테마 색으로 한 번만 다시 그린다
        val target = if (TraceTheme.isDark) DarkTraceColors else LightTraceColors
        val paint = MapPaint(target.mapLine, target.mapLand, target.mapSea, target.mapSelected, target.mapBorder)

        // 지도 본체(해안선·땅·방문 색·선택 테두리)는 비트맵으로 미리 그린다.
        // 복잡한 Path를 매 프레임 그리면 CPU 래스터화로 끊기므로, 조작 중에는 비트맵만 옮기고
        // 손을 뗀 뒤(150ms) 지금 배율로 선명한 비트맵을 다시 만든다.
        var base by remember { mutableStateOf<MapRaster?>(null) }
        var sharp by remember { mutableStateOf<MapRaster?>(null) }
        LaunchedEffect(map, visitedPaths, selected, wPx, hPx, dpx, paint) {
            if (wPx <= 0 || hPx <= 0) return@LaunchedEffect
            val geo = MapGeometry(k0, ox, oy, dpx, wPx, hPx)
            base = rasterize(map, visitedPaths, selected, paint, geo, 1f, 0f, 0f)
            sharp = null
            snapshotFlow { Triple(zoom.scale, zoom.offsetX, zoom.offsetY) }.collectLatest { (zs, zx, zy) ->
                delay(SHARPEN_DELAY_MS)
                sharp = if (zs == 1f && zx == 0f && zy == 0f) null
                else rasterize(map, visitedPaths, selected, paint, geo, zs, zx, zy)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "시군 단위 방문 지도, 색이 칠해진 곳이 가 본 지역이에요" }
                .pointerInput(zoom, map) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = viewConfiguration.touchSlop
                        var moved = false
                        var multi = false
                        var travel = Offset.Zero
                        var lastPos = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) {
                                val up = event.changes.firstOrNull { it.id == down.id }
                                if (!moved && !multi && up != null && !up.isConsumed) {
                                    lastPos = up.position
                                    val p = zoom.toContent(lastPos.x / density, lastPos.y / density)
                                    map.hitTest((p.x - ox) / k0, (p.y - oy) / k0, accept)?.let(select)
                                }
                                break
                            }
                            if (pressed.size >= 2) {
                                multi = true
                                moved = true
                                val c = event.calculateCentroid(useCurrent = true)
                                val pan = event.calculatePan()
                                zoom.zoomAt(c.x / density, c.y / density, zoom.scale * event.calculateZoom())
                                zoom.panBy(pan.x / density, pan.y / density)
                                event.changes.forEach { it.consume() }
                            } else {
                                val ch = pressed.first()
                                if (ch.isConsumed && !moved) {
                                    // 바깥 세로 스크롤이 가져갔다: 탭 아님
                                    moved = true
                                }
                                val d = ch.position - ch.previousPosition
                                travel += Offset(abs(d.x), abs(d.y))
                                if (travel.x + travel.y > slop) moved = true
                                // 1배에서는 한 손가락 드래그를 화면 스크롤에 넘긴다. 확대했을 때만 지도를 옮긴다
                                if (zoom.scale > 1f && ch.positionChanged()) {
                                    zoom.panBy(d.x / density, d.y / density)
                                    ch.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            val d = density
            val s = zoom.scale
            val tx = zoom.offsetX * d
            val ty = zoom.offsetY * d
            val b = base
            if (b == null) {
                // 첫 비트맵이 나오기 전 한 번만: 바로 그린다. 지도 좌표 → 화면 px: d·(s·(k0·p + o) + t)
                withTransform({
                    translate(tx, ty)
                    scale(s * d * k0, s * d * k0, pivot = Offset.Zero)
                    translate(ox / k0, oy / k0)
                }) { drawMap(map, visitedPaths, selected, paint) }
            } else {
                drawRaster(b, s, zoom.offsetX, zoom.offsetY)
                sharp?.let { drawRaster(it, s, zoom.offsetX, zoom.offsetY) }
            }
            // 라벨: 배율은 지도와 함께, 글자는 이미 화면 px로 쟀으므로 위치만 옮긴다
            withTransform({
                translate(tx, ty)
                scale(s, s, pivot = Offset.Zero)
            }) {
                fun at(x: Float, y: Float) = Offset(d * (k0 * x + ox), d * (k0 * y + oy))
                val halo = Stroke(width = 7f * d * k0, join = StrokeJoin.Round)
                groupLabels.forEach { (g, layout) ->
                    val p = at(g.labelX!!, g.labelY!!)
                    val tl = Offset(p.x - layout.size.width / 2f, p.y - layout.firstBaseline)
                    drawText(layout, color = colors.mapLand, topLeft = tl, drawStyle = halo)
                    // drawStyle을 밝혀 쓰지 않으면 앞의 테두리(Stroke) 스타일이 남는다
                    drawText(layout, color = colors.mapLabel, topLeft = tl, drawStyle = Fill)
                }
                insetLabels.forEach { (ins, layout) ->
                    val p = at(ins.labelX, ins.labelY)
                    val x = if (ins.labelCentered) p.x - layout.size.width / 2f else p.x
                    drawText(layout, color = colors.mapLabel, topLeft = Offset(x, p.y - layout.firstBaseline), drawStyle = Fill)
                }
            }
            // 지금 지역 링: 확대와 무관하게 같은 크기
            current?.let {
                val c = Offset(d * (s * (k0 * it.centerX + ox) + zoom.offsetX), d * (s * (k0 * it.centerY + oy) + zoom.offsetY))
                drawCircle(colors.mapLand, radius = 8f * d, center = c, style = Stroke(width = 4.4f * d))
                drawCircle(colors.mapSelected, radius = 8f * d, center = c, style = Stroke(width = 2.4f * d))
            }
        }
    }
}

private const val SHARPEN_DELAY_MS = 150L

private data class MapPaint(val line: Color, val land: Color, val sea: Color, val selected: Color, val border: Color)

/** 지역 사이 경계: 굵기 0.8, 3 그리고 2.4 간격 점선 (해안선 2.6보다 확실히 약하게) */
private val BorderStroke = Stroke(width = 0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2.4f)))

/** viewBox → 칸(dp) 맞춤과 픽셀 크기 */
private data class MapGeometry(val k0: Float, val ox: Float, val oy: Float, val density: Float, val wPx: Int, val hPx: Int)

/** (scale, x, y) 확대 상태로 칸 전체를 그린 비트맵 */
private class MapRaster(val image: ImageBitmap, val scale: Float, val x: Float, val y: Float)

private val rasterMutex = Mutex()

private suspend fun rasterize(
    map: RegionMap,
    visited: List<Pair<MapRegion, Color>>,
    selected: MapRegion?,
    paint: MapPaint,
    g: MapGeometry,
    s: Float,
    x: Float,
    y: Float,
): MapRaster = withContext(Dispatchers.Default) {
    rasterMutex.withLock {
        val image = ImageBitmap(g.wPx, g.hPx)
        CanvasDrawScope().draw(Density(g.density), LayoutDirection.Ltr, Canvas(image), Size(g.wPx.toFloat(), g.hPx.toFloat())) {
            drawRect(paint.sea)
            val d = g.density
            withTransform({
                translate(x * d, y * d)
                scale(s * d * g.k0, s * d * g.k0, pivot = Offset.Zero)
                translate(g.ox / g.k0, g.oy / g.k0)
            }) { drawMap(map, visited, selected, paint) }
        }
        MapRaster(image, s, x, y)
    }
}

/** 비트맵을 지금 확대 상태에 맞춰 옮기고 키워 그린다: 화면 = (s/s0)·비트맵 + d·(t − (s/s0)·t0) */
private fun DrawScope.drawRaster(r: MapRaster, s: Float, tx: Float, ty: Float) {
    val k = s / r.scale
    val d = density
    withTransform({
        translate(d * (tx - k * r.x), d * (ty - k * r.y))
        scale(k, k, pivot = Offset.Zero)
    }) { drawImage(r.image, filterQuality = FilterQuality.Low) }
}

/** 디자인 SVG 순서: 섬 박스 → 해안선(선색 2.6) → 땅색 → 방문 지역 색 → (추가) 지역 경계 점선 → 선택 지역 흰 테두리 2.4 */
private fun DrawScope.drawMap(
    map: RegionMap,
    visited: List<Pair<MapRegion, Color>>,
    selected: MapRegion?,
    paint: MapPaint,
) {
    val frameStroke = Stroke(width = 1.2f)
    map.insets.forEach { ins ->
        drawRect(paint.sea, topLeft = ins.frame.topLeft, size = ins.frame.size)
        drawRect(paint.line, topLeft = ins.frame.topLeft, size = ins.frame.size, style = frameStroke)
    }
    drawPath(map.allPath, paint.line, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
    drawPath(map.allPath, paint.land)
    visited.forEach { (r, c) -> drawPath(r.path, c) }
    drawPath(map.borderPath, paint.border, style = BorderStroke)
    if (selected != null && visited.any { it.first === selected }) {
        drawPath(selected.path, paint.selected, style = Stroke(width = 2.4f, join = StrokeJoin.Round))
    }
}

