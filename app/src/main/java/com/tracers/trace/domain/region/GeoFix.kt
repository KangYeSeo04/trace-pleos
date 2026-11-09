package com.tracers.trace.domain.region

/**
 * 현재 위치 한 번 (WGS84). 지역 판정에만 쓰고 저장하지 않는다(G-01: 위경도 좌표는 기기 안에서도 남기지 않음).
 *
 * @param timeMs 받은 시각 (epoch ms)
 */
data class GeoFix(
    val latitude: Double,
    val longitude: Double,
    val timeMs: Long,
) {
    /** 내비가 위치를 아직 못 잡으면 0,0이 올 수 있다. 위경도 범위 밖도 버린다 */
    val isValid: Boolean
        get() = !(latitude == 0.0 && longitude == 0.0) &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !latitude.isNaN() && !longitude.isNaN()
}
