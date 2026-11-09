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
import kotlin.math.hypot

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
) {
    val byCode: Map<String, MapRegion> = regions.associateBy { it.code }

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
        return RegionMap(vb.getDouble(0).toFloat(), vb.getDouble(1).toFloat(), regions, insets, groups, borderPath)
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
