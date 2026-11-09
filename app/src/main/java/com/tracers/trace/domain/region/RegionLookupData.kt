package com.tracers.trace.domain.region

/**
 * 판정용 경계 데이터 (assets/regions_lookup.json, tools/geo/build_regions.py로 생성).
 * 좌표는 WGS84 경도·위도. 고리는 평평한 [lon, lat, lon, lat, …], fill rule evenOdd.
 *
 * @property regions 161 지역 (화면·방문 개수 단위)
 * @property units 법정 시·군·구 (일반구 포함, 판정 단위)
 */
class RegionLookupData(
    val regions: List<RegionInfo>,
    val units: List<UnitShape>,
) {
    val regionByCode: Map<String, RegionInfo> = regions.associateBy { it.code }

    val unitByCode: Map<String, UnitShape> = units.associateBy { it.code }

    /** 지역 하나에 속한 시·군·구 수. 1곳뿐이면 표시 이름에 구 이름을 붙이지 않는다 */
    internal val unitCountByRegion: Map<String, Int> = units.groupingBy { it.regionCode }.eachCount()
}

/**
 * 161 지역 하나.
 * @property group 채색 묶음 (seoul, gg, gw, cb, cn, jb, jn, gb, gn, jj)
 */
data class RegionInfo(
    val code: String,
    val name: String,
    val sido: String,
    val group: String,
)

/** 시·군·구 하나의 경계 */
class UnitShape(
    val code: String,
    /** "중랑구", "분당구", "울릉군" */
    val name: String,
    /** "서울", "경기" */
    val sidoShort: String,
    /** 속한 161 지역 코드 */
    val regionCode: String,
    /** [minLon, minLat, maxLon, maxLat] */
    val bbox: DoubleArray,
    /** [lon, lat] 대표점 (홈 타일 미니 지도 점) */
    val center: DoubleArray,
    val rings: List<DoubleArray>,
)
