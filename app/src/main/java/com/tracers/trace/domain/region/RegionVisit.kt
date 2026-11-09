package com.tracers.trace.domain.region

/**
 * 161 지역 하나의 방문 기록 (G-03). 위경도와 시·군·구(구 이름)는 남기지 않는다.
 *
 * @property firstVisitedAtMs 처음 간 시각
 * @property lastVisitedAtMs 마지막으로 있었던 시각
 */
data class RegionVisit(
    val regionCode: String,
    val firstVisitedAtMs: Long,
    val lastVisitedAtMs: Long,
    val distanceKm: Double,
    val stayMs: Long,
)
