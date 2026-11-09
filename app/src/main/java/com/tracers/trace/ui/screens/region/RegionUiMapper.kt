package com.tracers.trace.ui.screens.region

import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisit

/** 방문 기록 + 경계 데이터 → 지역 화면 상태 */
internal object RegionUiMapper {

    /** 경계 데이터에 없는 코드(데이터 개정으로 사라진 지역)는 뺀다. 순서는 최근에 간 순 그대로 */
    fun visited(visits: List<RegionVisit>, data: RegionLookupData?): List<VisitedRegion> {
        if (data == null) return emptyList()
        return visits.mapNotNull { v ->
            val r = data.regionByCode[v.regionCode] ?: return@mapNotNull null
            VisitedRegion(
                code = r.code,
                name = r.name,
                sidoName = r.sido,
                distanceKm = v.distanceKm,
                stayMinutes = v.stayMs / 60_000,
            )
        }
    }

    fun currentCode(status: RegionStatus): String? = (status as? RegionStatus.Located)?.match?.regionCode
}
