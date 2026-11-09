package com.tracers.trace.domain.region

/** 지금 지역 판정 상태 (홈 지역 타일·지역 화면 지금 지역 링) */
sealed interface RegionStatus {
    /** 위치 기록 꺼짐 (G-04). 위치를 요청하지 않는다 */
    data object Off : RegionStatus
    /** 켜진 뒤 아직 첫 위치를 기다리는 중 */
    data object Searching : RegionStatus
    /** 위치가 한동안 안 온다 (내비 연결 실패·GPS 없음) */
    data object Unavailable : RegionStatus
    /** 위치는 왔지만 161 지역 밖 (먼바다·국외) */
    data object Outside : RegionStatus
    data class Located(val match: RegionMatch) : RegionStatus
}
