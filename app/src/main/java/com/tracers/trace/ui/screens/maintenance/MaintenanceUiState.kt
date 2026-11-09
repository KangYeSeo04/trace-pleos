package com.tracers.trace.ui.screens.maintenance

import java.time.LocalDate
import java.time.YearMonth

/**
 * 정비 상세 화면 (S04) + 기록 추가 (S05) 상태. 디자인: design/html/MaintDark.dc.html (600 × 2600)
 *
 * 기본값은 빈 상태(방문 감지 없음, 점검 항목·정비 기록 없음). 앱 실행 화면은 SDK 연결 전까지 이 상태로 뜬다.
 * 화면은 이 값만 받아 그리고, 주기 계산(D-01)·방문 감지(D-03)는 2단계 domain/에서 한다.
 */
data class MaintenanceUiState(
    /** 방문 감지 카드 (D-03). null이면 카드를 숨긴다 */
    val visit: VisitDetection? = null,
    /** 점검 항목. 급한 순으로 정렬된 상태로 받는다 (D-01) */
    val items: List<MaintenanceItem> = emptyList(),
    /** 정비 기록. 최근 순 (D-04, 2회차) */
    val records: List<MaintenanceRecord> = emptyList(),
    /** 정비 달력 (2회차). null이면 기록이 없어 이번 달을 빈 달력으로 */
    val calendar: MaintenanceCalendar? = null,
    /** 기록 추가 양식 (S05, 2회차) */
    val form: MaintenanceRecordForm = MaintenanceRecordForm(),
)

/**
 * 방문 감지 카드: "어제 [정비소 이름]에 1시간 12분 머물렀어요".
 *
 * @param whenLabel 방문 시점 ("어제", "11월 5일")
 * @param placeName 머문 장소 이름
 * @param stayLabel 머문 시간 ("1시간 12분")
 */
data class VisitDetection(
    val whenLabel: String,
    val placeName: String,
    val stayLabel: String,
) {
    val title: String get() = "$whenLabel ${placeName}에 $stayLabel 머물렀어요"
}

/**
 * 점검 항목 한 줄.
 *
 * @param name 항목 이름 ("와이퍼 블레이드")
 * @param remaining 남은 값 ("12일 남음", "1,200 km 남음")
 * @param gauges 사용률 막대. 기준이 하나면 1개, 거리·기간 두 기준이면 2개
 */
data class MaintenanceItem(
    val name: String,
    val remaining: String,
    val gauges: List<UsageGauge>,
) {
    /** 남은 값 글자색은 가장 많이 쓴 막대 기준 */
    val level: UsageLevel get() = UsageLevel.of(gauges.maxOfOrNull { it.percent } ?: 0f)
}

/**
 * 사용률 막대 하나.
 *
 * @param percent 사용률 0~100
 * @param basis 기준 값 ("11.6 / 12개월", "8,800 / 10,000 km")
 */
data class UsageGauge(
    val percent: Float,
    val basis: String,
) {
    val level: UsageLevel get() = UsageLevel.of(percent)
}

/** 사용률 단계 (D-01: 경고 ≥ 90%, 주의 ≥ 75%, 그 외 회색) */
enum class UsageLevel {
    Normal, Caution, Warn;

    companion object {
        const val WARN_PERCENT = 90f
        const val CAUTION_PERCENT = 75f

        fun of(percent: Float): UsageLevel = when {
            percent >= WARN_PERCENT -> Warn
            percent >= CAUTION_PERCENT -> Caution
            else -> Normal
        }
    }
}

/** 기록 입력 방식. 자동 감지는 파랑, 직접 입력은 회색 */
enum class RecordInputMethod(val label: String) {
    AutoDetected("자동 감지"),
    Manual("직접 입력"),
}

/**
 * 정비 기록 한 건 (2회차).
 *
 * @param itemNames 이 기록이 갱신한 점검 항목 이름. 항목 행을 펼치면 이 값으로 거른다
 * @param costWon 금액. null이면 입력 안 함
 */
data class MaintenanceRecord(
    val id: Long,
    val date: LocalDate,
    val odometerKm: Int,
    val title: String,
    val place: String,
    val method: RecordInputMethod,
    val itemNames: List<String>,
    val costWon: Int? = null,
)

/**
 * 정비 달력 (2회차).
 *
 * @param month 보여 줄 달. 처음엔 가장 최근 기록이 있는 달
 * @param selectedDate 고른 날. null이면 달 요약("정비 N건")
 */
data class MaintenanceCalendar(
    val month: YearMonth,
    val selectedDate: LocalDate? = null,
)

/**
 * 기록 추가 양식 (S05). 입력 중인 글자를 그대로 들고 있고, 저장할 때 값으로 바꾼다.
 *
 * @param dateText 날짜 ("2026. 11. 5"). 기본 오늘, 방문 감지면 방문일
 * @param odometerDigits 주행거리 숫자만 ("38800"). 화면에서 "38,800 km"로 보인다
 * @param place 장소. 방문 감지면 자동 입력
 * @param itemOptions 항목 칩 목록 (디자인 순서)
 * @param selectedItems 고른 항목. 최소 1개여야 저장 가능
 * @param costDigits 비용 숫자만 (선택). 화면에서 "80,000원"으로 보인다
 * @param memo 메모 (선택, [MEMO_MAX]자까지). 디자인엔 없고 기획안 S05-6에서 추가
 * @param isEditing 기존 기록 수정이면 true (제목 "기록 수정")
 */
data class MaintenanceRecordForm(
    val dateText: String = "",
    val odometerDigits: String = "",
    val place: String = "",
    val itemOptions: List<String> = ITEM_OPTIONS,
    val selectedItems: Set<String> = emptySet(),
    val costDigits: String = "",
    val memo: String = "",
    val isEditing: Boolean = false,
) {
    val canSave: Boolean get() = selectedItems.isNotEmpty()

    companion object {
        /** 디자인의 항목 칩 6개 */
        val ITEM_OPTIONS = listOf("타이어 위치 교환", "에어컨 필터", "와이퍼", "브레이크", "12V 배터리", "기타")
        const val MEMO_MAX = 200
    }
}
