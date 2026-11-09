package com.tracers.trace.domain.region

import kotlin.math.cos
import kotlin.math.hypot

/**
 * 판정 결과.
 *
 * @property unitCode 법정 시·군·구 코드 (일반구 포함)
 * @property unitName "중랑구", "분당구". 지금 상태 표시에만 쓰고 저장하지 않는다
 * @property regionCode 161 지역 코드 (방문 기록 단위)
 * @property snapped 어느 경계에도 안 들어가 가장 가까운 시·군·구에 붙였는지 (해안·다리·바다 위 GPS)
 */
data class RegionMatch(
    val unitCode: String,
    val unitName: String,
    val regionCode: String,
    val regionName: String,
    val sido: String,
    val group: String,
    /** 지역에 시·군·구가 여럿일 때만 구 이름을 붙인다 ("서울 중랑구", "성남시 분당구", "울릉군") */
    val showUnit: Boolean,
    val snapped: Boolean,
) {
    val label: String get() = if (showUnit) "$regionName $unitName" else regionName
}

/**
 * 위경도 → 법정 시·군·구 → 161 지역 (G-01).
 *
 * 1. 직전에 맞은 시·군·구부터 확인 (대부분 같은 곳이라 고리 하나만 검사)
 * 2. bbox에 들어가는 후보만 점-다각형 검사 (evenOdd)
 * 3. 어디에도 안 들어가면 [SNAP_MAX_M] 안의 가장 가까운 경계에 붙인다. 더 멀면 null (먼바다·국외)
 *
 * 데이터는 앱 시작 때 한 번 읽어 넘긴다. 스레드 안전하지 않다 (직전 결과를 기억한다).
 */
class RegionLocator(private val data: RegionLookupData) {

    private var last: UnitShape? = null

    fun locate(latitude: Double, longitude: Double): RegionMatch? {
        val lon = longitude
        val lat = latitude
        last?.let { if (contains(it, lon, lat)) return match(it, snapped = false) }

        for (u in data.units) {
            if (u !== last && inBox(u.bbox, lon, lat, 0.0) && contains(u, lon, lat)) {
                last = u
                return match(u, snapped = false)
            }
        }
        val near = nearest(lon, lat) ?: return null
        last = near
        return match(near, snapped = true)
    }

    private fun match(u: UnitShape, snapped: Boolean): RegionMatch {
        val r = data.regionByCode.getValue(u.regionCode)
        return RegionMatch(
            unitCode = u.code,
            unitName = u.name,
            regionCode = r.code,
            regionName = r.name,
            sido = r.sido,
            group = r.group,
            showUnit = (data.unitCountByRegion[r.code] ?: 1) > 1,
            snapped = snapped,
        )
    }

    private fun contains(u: UnitShape, x: Double, y: Double): Boolean {
        if (!inBox(u.bbox, x, y, 0.0)) return false
        var inside = false
        for (r in u.rings) {
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

    /** 경계선까지 가장 가까운 시·군·구. 점 근처를 평면으로 보고 m 단위로 잰다 */
    private fun nearest(lon: Double, lat: Double): UnitShape? {
        val kx = cos(Math.toRadians(lat)) * M_PER_DEG_LON_EQ
        val marginDeg = SNAP_MAX_M / M_PER_DEG_LAT
        var best: UnitShape? = null
        var bestD = SNAP_MAX_M
        for (u in data.units) {
            // 경도 쪽 여유는 위도에서 cos만큼 넓다 (한반도 위도 33~39°에서 1.3배 안쪽)
            if (!inBox(u.bbox, lon, lat, marginDeg * 1.3, marginDeg)) continue
            for (r in u.rings) {
                val n = r.size / 2
                for (i in 0 until n) {
                    val j = if (i + 1 == n) 0 else i + 1
                    val ax = (r[2 * i] - lon) * kx; val ay = (r[2 * i + 1] - lat) * M_PER_DEG_LAT
                    val bx = (r[2 * j] - lon) * kx; val by = (r[2 * j + 1] - lat) * M_PER_DEG_LAT
                    val d = segmentDistance(ax, ay, bx, by)
                    if (d < bestD) { bestD = d; best = u }
                }
            }
        }
        return best
    }

    /** 원점에서 선분 AB까지 거리 */
    private fun segmentDistance(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax; val dy = by - ay
        val l2 = dx * dx + dy * dy
        val t = if (l2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
        return hypot(ax + t * dx, ay + t * dy)
    }

    private fun inBox(b: DoubleArray, x: Double, y: Double, mx: Double, my: Double = mx): Boolean =
        x >= b[0] - mx && x <= b[2] + mx && y >= b[1] - my && y <= b[3] + my

    companion object {
        /** 경계 밖 GPS를 가까운 시·군·구에 붙이는 최대 거리. 다리·해안 도로·여객선 근해까지 */
        const val SNAP_MAX_M = 10_000.0
        private const val M_PER_DEG_LAT = 110_540.0
        private const val M_PER_DEG_LON_EQ = 111_320.0
    }
}
