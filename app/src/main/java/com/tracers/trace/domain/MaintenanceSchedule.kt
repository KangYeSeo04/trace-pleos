package com.tracers.trace.domain

/**
 * 점검 항목 8개와 기본 주기 (D-01). 주기 값은 이 파일 한 곳에만 둔다.
 *
 * 출처: design/html/MaintDark.dc.html 점검 항목 목록의 기준 값(막대 오른쪽 "사용 / 기준").
 * 1단계에서 SampleData.maintenanceItems로 옮겨 둔 값과 같다.
 * 디자인은 항목마다 기준이 하나(거리 또는 기간)라 나머지 쪽은 null. 둘 다 있으면 먼저 오는 쪽을 쓴다.
 *
 * @param displayName 화면에 보이는 이름 (디자인 그대로)
 * @param chipLabel 기록 추가 양식(S05) 항목 칩 글자
 * @param intervalKm 거리 주기 (km). null이면 거리 기준 없음
 * @param intervalMonths 기간 주기 (개월). null이면 기간 기준 없음
 */
enum class MaintenanceItemType(
    val displayName: String,
    val chipLabel: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
) {
    /** 디자인: "11.6 / 12개월" */
    WIPER("와이퍼 블레이드", "와이퍼", intervalKm = null, intervalMonths = 12),
    /** 디자인: "8,800 / 10,000 km" */
    CABIN_FILTER("에어컨 필터", "에어컨 필터", intervalKm = 10_000, intervalMonths = null),
    /** 디자인: "7,900 / 10,000 km" */
    TIRE_ROTATION("타이어 위치 교환", "타이어 위치 교환", intervalKm = 10_000, intervalMonths = null),
    /** 디자인: "17 / 24개월" */
    BRAKE_FLUID("브레이크액", "브레이크액", intervalKm = null, intervalMonths = 24),
    /** 디자인: "24 / 48개월" */
    BATTERY_12V("12V 배터리", "12V 배터리", intervalKm = null, intervalMonths = 48),
    /** 디자인: "38,800 / 65,000 km" */
    BRAKE_PAD_FRONT("브레이크 패드 (앞)", "브레이크 패드", intervalKm = 65_000, intervalMonths = null),
    /** 디자인: "38,800 / 60,000 km" */
    REDUCER_OIL("감속기 오일 점검", "감속기 오일", intervalKm = 60_000, intervalMonths = null),
    /** 디자인: "38,800 / 200,000 km" */
    COOLANT("냉각수 (첫 교체)", "냉각수", intervalKm = 200_000, intervalMonths = null);

    companion object {
        fun ofChip(label: String): MaintenanceItemType? = entries.firstOrNull { it.chipLabel == label }
    }
}

/** 점검 임박(D-02) 기준과 사용률 단계(D-01) */
object MaintenanceThresholds {
    /** 사용률 이 이상이면 경고 */
    const val WARN_RATIO = 0.90
    /** 사용률 이 이상이면 주의 */
    const val CAUTION_RATIO = 0.75
    /** 남은 기간이 이 이하이면 점검 임박 */
    const val IMMINENT_DAYS = 14L
    /** 남은 거리가 이 이하이면 점검 임박 */
    const val IMMINENT_KM = 500.0
}
