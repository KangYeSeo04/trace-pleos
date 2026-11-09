package com.tracers.trace.ui.screens.region

import androidx.compose.runtime.Immutable

/**
 * 지역 상세 화면(S08) 상태.
 *
 * 지도 모양은 assets/regions_display.json에서 따로 읽는다. 여기에는 기록에서 나온 값만 둔다.
 * 지역 코드는 regions_display.json의 161 지역 code(예: 서울 "11000", 경주시 "47130").
 *
 * @param visited 방문한 지역(색칠할 지역)
 * @param currentRegionCode 지금 있는 지역(흰 링). 모르면 null
 * @param selectedRegionCode 사용자가 고른 지역. null이면 지금 지역을 보여 준다
 * @param locationRecordingOn 위치 기록 켜짐(G-04). 끄면 새 기록만 멈춘다
 */
@Immutable
data class RegionUiState(
    val visited: List<VisitedRegion> = emptyList(),
    val currentRegionCode: String? = null,
    val selectedRegionCode: String? = null,
    val locationRecordingOn: Boolean = true,
) {
    /** 카드에 보여 줄 지역: 고른 지역 → 지금 지역 → 없음 */
    val selected: VisitedRegion?
        get() = visited.find { it.code == selectedRegionCode }
            ?: visited.find { it.code == currentRegionCode }

    val visitedCount: Int get() = visited.size
}

/**
 * 방문한 지역 하나. 색은 지도 데이터의 시·도 묶음(group)으로 정한다(TraceColors.regionGroup).
 *
 * @param distanceKm 이 지역에서 달린 거리
 * @param stayMinutes 이 지역에 머문 시간(분)
 */
@Immutable
data class VisitedRegion(
    val code: String,
    val name: String,
    val sidoName: String,
    val distanceKm: Double,
    val stayMinutes: Long,
)
