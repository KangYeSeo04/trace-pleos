package com.tracers.trace.ui.screens.home

import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.RegionMatch
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisit
import com.tracers.trace.ui.screens.region.RegionFormat
import kotlin.math.roundToInt

/**
 * 홈 지역 타일 (기획안 홈 ⑦): 지금 지역 + 구 이름, 방문 수, 방문 비율 막대, 누적 거리.
 * 위치를 못 받거나 기록이 꺼져 있으면 상태 줄에 그 사실을 쓴다 (방문 수·거리는 아래 줄로).
 */
internal object RegionTileMapper {

    fun map(status: RegionStatus, visits: List<RegionVisit>, data: RegionLookupData?, isDriving: Boolean): TileState.Region {
        val total = data?.regions?.size ?: 0
        val visited = if (data == null) 0 else visits.count { it.regionCode in data.regionByCode }
        val km = visits.sumOf { it.distanceKm }
        val countText = if (total > 0) "${total}곳 중 ${visited}곳 방문" else ""
        val distanceText = "달린 거리 ${RegionFormat.distance(km)}"
        val bar = if (total > 0) visited * 100f / total else 0f

        val match = (status as? RegionStatus.Located)?.match
        if (match == null || data == null) {
            val (msg, level) = when (status) {
                RegionStatus.Off -> "위치 기록이 꺼져 있어요" to StatusLevel.Normal
                RegionStatus.Unavailable -> "위치를 받지 못하고 있어요" to StatusLevel.Caution
                RegionStatus.Outside -> "지도 밖에 있어요" to StatusLevel.Normal
                else -> "위치를 찾는 중이에요" to StatusLevel.Normal
            }
            return TileState.Region(
                city = "–",
                district = "",
                showMap = false,
                status = msg,
                statusLevel = level,
                barPercent = bar,
                subText = listOf(countText, RegionFormat.distance(km)).filter { it.isNotEmpty() }.joinToString(" · "),
            )
        }

        val district = if (match.showUnit) match.unitName else ""
        val dot = seoulDot(match, data)
        return TileState.Region(
            city = match.regionName,
            district = district,
            showMap = dot != null,
            locationX = dot?.first ?: 50f,
            locationY = dot?.second ?: 40f,
            value = match.regionName,
            unit = district,
            // 주행 중에는 상태 1줄만 보인다: 구 이름이 있으면 구 이름, 없으면 방문 수
            status = if (isDriving && district.isNotEmpty()) district else countText,
            statusLevel = StatusLevel.Normal,
            barPercent = bar,
            subText = distanceText,
        )
    }

    /**
     * 서울 미니 지도(100 × 80, Tile.SeoulMap) 위 점 자리. 서울에 있을 때만, 구 대표점을 서울 경계 상자에 맞춰 놓는다(근사).
     * 미니 지도 외곽선은 x 4~92, y 3~77 안에 그려져 있다.
     */
    internal fun seoulDot(match: RegionMatch, data: RegionLookupData): Pair<Float, Float>? {
        if (match.group != SEOUL_GROUP) return null
        val unit = data.unitByCode[match.unitCode] ?: return null
        val seoul = data.units.filter { it.regionCode == match.regionCode }
        val minLon = seoul.minOf { it.bbox[0] }; val maxLon = seoul.maxOf { it.bbox[2] }
        val minLat = seoul.minOf { it.bbox[1] }; val maxLat = seoul.maxOf { it.bbox[3] }
        val x = 4f + ((unit.center[0] - minLon) / (maxLon - minLon) * 88).toFloat()
        val y = 3f + ((maxLat - unit.center[1]) / (maxLat - minLat) * 74).toFloat()
        return (x * 10).roundToInt() / 10f to (y * 10).roundToInt() / 10f
    }

    private const val SEOUL_GROUP = "seoul"
}
