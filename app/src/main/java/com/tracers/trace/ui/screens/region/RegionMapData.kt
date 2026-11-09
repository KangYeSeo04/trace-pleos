package com.tracers.trace.ui.screens.region

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.tan

/**
 * 화면용 지도(assets/regions_display.json, tools/geo/build_regions.py로 생성).
 * 좌표는 디자인 지도 viewBox(563.3 × 612) 단위. Path는 만들 때 한 번만 생성하고 그리는 동안 다시 만들지 않는다.
 */
class RegionMap(
    val width: Float,
    val height: Float,
    val regions: List<MapRegion>,
    val insets: List<MapInset>,
    val groups: List<MapGroup>,
    /** 지역 사이 경계(해안선 제외) 선들 */
    val borderPath: Path,
    /** 섬 박스별 위경도 → 지도 좌표 [s, ox, oy] (meta.transforms). 없으면 빈 맵 */
    val transforms: Map<String, FloatArray> = emptyMap(),
) {
    val byCode: Map<String, MapRegion> = regions.associateBy { it.code }

    /** 위경도를 [transform] 박스의 지도 좌표로 (build_regions.py와 같은 메르카토르) */
    fun project(transform: String, latitude: Double, longitude: Double): Offset? {
        val t = transforms[transform] ?: return null
        val x = Math.toRadians(longitude) * t[0] + t[1]
        val y = -ln(tan(PI / 4 + Math.toRadians(latitude) / 2)) * t[0] + t[2]
        return Offset(x.toFloat(), y.toFloat())
    }

    /**
     * 홈 지역 타일의 작은 지도: 지역 [code]의 모양(지금 위치가 있는 박스의 고리만)과 지금 위치 점.
     * 위치가 그 지역 모양 밖으로 나오면(해안·단순화 오차) 점은 모양 안쪽 가장자리로 붙이지 않고 그대로 둔다.
     */
    fun miniMap(code: String, latitude: Double?, longitude: Double?): MiniMap? {
        val r = byCode[code] ?: return null
        // 이 지역이 놓인 박스들: 본토(main) 또는 섬 박스, 독도는 울릉 박스 안의 다른 배율
        val frames = insets.filter { code in it.regions }
        fun frameOf(ring: FloatArray): String {
            val b = ringBounds(ring)
            return frames.firstOrNull { it.frame.contains(b.topLeft) && it.frame.contains(b.bottomRight) }?.id ?: MAIN
        }
        val ringFrames = r.rings.map(::frameOf)
        val candidates = (listOf(r.inset) + frames.map { it.id } + MAIN + (if (r.inset == ULLEUNG) listOf(DOKDO) else emptyList()))
            .distinct().filter { it in transforms }
        var dot: Offset? = null
        var frame = if (r.inset == ULLEUNG) ULLEUNG else ringFrames.firstOrNull { it == r.inset } ?: MAIN
        if (latitude != null && longitude != null) {
            val hit = candidates.firstNotNullOfOrNull { t ->
                project(t, latitude, longitude)?.takeIf { r.contains(it.x, it.y) }?.let { t to it }
            }
            val (t, p) = hit ?: (r.inset to project(r.inset, latitude, longitude))
            dot = p
            frame = if (t == DOKDO) ULLEUNG else t
        }
        val rings = r.rings.filterIndexed { i, _ -> ringFrames[i] == frame }.ifEmpty { r.rings }
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            rings.forEach { ring ->
                moveTo(ring[0], ring[1])
                var i = 2
                while (i < ring.size) { lineTo(ring[i], ring[i + 1]); i += 2 }
                close()
            }
        }
        return MiniMap(path, path.getBounds(), dot)
    }

    private fun ringBounds(ring: FloatArray): Rect {
        var x0 = Float.MAX_VALUE; var y0 = Float.MAX_VALUE; var x1 = -Float.MAX_VALUE; var y1 = -Float.MAX_VALUE
        var i = 0
        while (i < ring.size) {
            x0 = minOf(x0, ring[i]); x1 = maxOf(x1, ring[i]); y0 = minOf(y0, ring[i + 1]); y1 = maxOf(y1, ring[i + 1])
            i += 2
        }
        return Rect(x0, y0, x1, y1)
    }

    private companion object {
        const val MAIN = "main"
        const val ULLEUNG = "ulleung"
        const val DOKDO = "dokdo"
    }

    /** 모든 지역을 합친 Path: 해안선(선색 굵게) → 땅색 채우기를 한 번씩만 그리려고 쓴다 */
    val allPath: Path = Path().apply {
        fillType = PathFillType.EvenOdd
        regions.forEach { addPath(it.path) }
    }

    /**
     * 지도 좌표 (x, y)에 있는 지역. [accept]가 false인 지역은 건너뛴다(방문 안 한 지역 등).
     * 섬 박스 틀 안을 누르면 섬을 정확히 맞추지 않아도 그 박스의 지역을 고른다.
     */
    fun hitTest(x: Float, y: Float, accept: (String) -> Boolean): String? {
        regions.firstOrNull { accept(it.code) && it.contains(x, y) }?.let { return it.code }
        val inset = insets.firstOrNull { it.frame.contains(Offset(x, y)) } ?: return null
        return inset.regions.filter(accept).mapNotNull { byCode[it] }
            .minByOrNull { r -> if (r.inset == inset.id) hypot(r.centerX - x, r.centerY - y) else Float.MAX_VALUE / 2 }
            ?.code
    }
}

/** 홈 타일 작은 지도: 지역 모양(지도 좌표)과 그 범위, 지금 위치(없으면 null) */
class MiniMap(val path: Path, val bounds: Rect, val dot: Offset?)

class MapRegion(
    val code: String,
    val name: String,
    val sido: String,
    val group: String,
    /** 가장 큰 고리가 놓인 곳: main · ulleung · jeju · baengnyeong · heuksan */
    val inset: String,
    val centerX: Float,
    val centerY: Float,
    /** 고리별 [x0, y0, x1, y1, …] (탭 판정용) */
    val rings: List<FloatArray>,
    val path: Path,
    val bounds: Rect,
) {
    fun contains(x: Float, y: Float): Boolean {
        if (!bounds.contains(Offset(x, y))) return false
        var inside = false
        for (r in rings) {
            val n = r.size / 2
            var j = n - 1
            for (i in 0 until n) {
                val xi = r[2 * i]; val yi = r[2 * i + 1]; val xj = r[2 * j]; val yj = r[2 * j + 1]
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
        }
        return inside
    }
}

class MapInset(
    val id: String,
    val regions: List<String>,
    val label: String,
    val labelX: Float,
    val labelY: Float,
    /** true면 라벨 가운데 정렬, false면 왼쪽 정렬 (SVG text-anchor) */
    val labelCentered: Boolean,
    val frame: Rect,
)

class MapGroup(val id: String, val name: String, val regionCount: Int, val labelX: Float?, val labelY: Float?)

/** 지도 데이터를 한 번만 읽어 프로세스 안에서 같이 쓴다. */
object RegionMapStore {
    private const val ASSET = "regions_display.json"
    private val mutex = Mutex()

    @Volatile
    private var cached: RegionMap? = null

    suspend fun load(context: Context): RegionMap = cached ?: mutex.withLock {
        cached ?: withContext(Dispatchers.Default) { read(context.applicationContext) }.also { cached = it }
    }

    /** @Preview 전용: 미리보기는 코루틴이 돌지 않아 바로 읽는다 */
    fun loadBlockingForPreview(context: Context): RegionMap = cached ?: read(context).also { cached = it }

    private fun read(context: Context): RegionMap {
        val text = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parse(JSONObject(text))
    }

    internal fun parse(root: JSONObject): RegionMap {
        val vb = root.getJSONArray("viewBox")
        val regions = root.getJSONArray("regions").objects().map { o ->
            val rings = parsePathData(o.getString("d"))
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                rings.forEach { r ->
                    moveTo(r[0], r[1])
                    var i = 2
                    while (i < r.size) { lineTo(r[i], r[i + 1]); i += 2 }
                    close()
                }
            }
            val c = o.getJSONArray("center")
            MapRegion(
                code = o.getString("code"), name = o.getString("name"), sido = o.getString("sido"),
                group = o.getString("group"), inset = o.getString("inset"),
                centerX = c.getDouble(0).toFloat(), centerY = c.getDouble(1).toFloat(),
                rings = rings, path = path, bounds = path.getBounds(),
            )
        }
        val insets = root.getJSONArray("insets").objects().map { o ->
            val f = o.getJSONArray("frame")
            val lp = o.getJSONArray("labelPos")
            MapInset(
                id = o.getString("id"),
                regions = o.getJSONArray("regions").strings(),
                label = o.getString("label"),
                labelX = lp.getDouble(0).toFloat(), labelY = lp.getDouble(1).toFloat(),
                labelCentered = o.optString("labelAnchor", "middle") == "middle",
                frame = Rect(f.getDouble(0).toFloat(), f.getDouble(1).toFloat(), f.getDouble(2).toFloat(), f.getDouble(3).toFloat()),
            )
        }
        val groups = root.getJSONArray("groups").objects().map { o ->
            val lp = o.optJSONArray("labelPos")
            MapGroup(
                id = o.getString("id"), name = o.getString("name"), regionCount = o.getInt("regions"),
                labelX = lp?.getDouble(0)?.toFloat(), labelY = lp?.getDouble(1)?.toFloat(),
            )
        }
        val borderPath = Path().apply {
            parsePathData(root.optString("borders", ""), minPoints = 2).forEach { l ->
                moveTo(l[0], l[1])
                var i = 2
                while (i < l.size) { lineTo(l[i], l[i + 1]); i += 2 }
            }
        }
        val transforms = root.optJSONObject("meta")?.optJSONObject("transforms")?.let { t ->
            t.keys().asSequence().associateWith { k ->
                val a = t.getJSONArray(k)
                FloatArray(3) { a.getDouble(it).toFloat() }
            }
        }.orEmpty()
        return RegionMap(vb.getDouble(0).toFloat(), vb.getDouble(1).toFloat(), regions, insets, groups, borderPath, transforms)
    }

    /** "M x y L x y x y … Z M …" (build_regions.py 출력 형식) → 고리(또는 선)별 좌표 배열. 점이 [minPoints]개 미만이면 버린다 */
    internal fun parsePathData(d: String, minPoints: Int = 3): List<FloatArray> {
        val rings = ArrayList<FloatArray>()
        var cur = FloatArrayBuilder()
        var i = 0
        val n = d.length
        while (i < n) {
            val ch = d[i]
            when {
                ch == 'M' -> { if (cur.size > 0) rings += cur.build(); cur = FloatArrayBuilder(); i++ }
                ch == 'Z' -> { if (cur.size > 0) rings += cur.build(); cur = FloatArrayBuilder(); i++ }
                ch == 'L' || ch == ' ' || ch == ',' -> i++
                else -> {
                    var j = i
                    while (j < n && (d[j].isDigit() || d[j] == '.' || d[j] == '-')) j++
                    cur.add(d.substring(i, j).toFloat())
                    i = j
                }
            }
        }
        if (cur.size > 0) rings += cur.build()
        return rings.filter { it.size >= minPoints * 2 }
    }

    private class FloatArrayBuilder {
        private var a = FloatArray(64)
        var size = 0
            private set

        fun add(v: Float) {
            if (size == a.size) a = a.copyOf(a.size * 2)
            a[size++] = v
        }

        fun build(): FloatArray = a.copyOf(size)
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = List(length()) { getString(it) }
}
