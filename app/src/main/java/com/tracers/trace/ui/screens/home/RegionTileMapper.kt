package com.tracers.trace.ui.screens.home

import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.GeoFix
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisit
import com.tracers.trace.ui.screens.region.RegionFormat

/**
 * 홈 지역 타일 (기획안 홈 ⑦): 지금 지역 + 구 이름, 방문 수, 방문 비율 막대, 누적 거리.
 * 정차 중에는 지금 지역 모양과 지금 위치 점을 작은 지도로 보인다(어느 지역이든).
 * 위치를 못 받거나 기록이 꺼져 있으면 상태 줄에 그 사실을 쓴다 (방문 수·거리는 아래 줄로).
 */
internal object RegionTileMapper {

    fun map(
        status: RegionStatus,
        visits: List<RegionVisit>,
        data: RegionLookupData?,
        isDriving: Boolean,
        location: GeoFix? = null,
    ): TileState.Region {
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
                status = msg,
                statusLevel = level,
                barPercent = bar,
                subText = listOf(countText, RegionFormat.distance(km)).filter { it.isNotEmpty() }.joinToString(" · "),
            )
        }

        val district = if (match.showUnit) match.unitName else ""
        val fix = location?.takeIf { it.isValid }
        return TileState.Region(
            city = match.regionName,
            district = district,
            mapCode = match.regionCode,
            mapLatitude = fix?.latitude,
            mapLongitude = fix?.longitude,
            value = match.regionName,
            unit = district,
            // 주행 중에는 상태 1줄만 보인다: 구 이름이 있으면 구 이름, 없으면 방문 수
            status = if (isDriving && district.isNotEmpty()) district else countText,
            statusLevel = StatusLevel.Normal,
            barPercent = bar,
            subText = distanceText,
        )
    }

}
