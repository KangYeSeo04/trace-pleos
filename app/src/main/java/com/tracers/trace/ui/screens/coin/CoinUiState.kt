package com.tracers.trace.ui.screens.coin

/**
 * 코인 상세 화면 (S06) 상태. 디자인: design/html/CoinDark.dc.html
 *
 * 기본값은 빈 상태(주행 기록 없음, 받을 코인 없음). 앱 실행 화면은 SDK 연결 전까지 이 상태로 뜬다.
 * 화면은 이 값만 받아 그리고, 계산(안전점수·적립)은 2단계 domain/에서 한다.
 */
data class CoinUiState(
    /** 보유 코인 */
    val balance: Int = 0,
    /** 이번 주행에서 모은 별 수. null이면 아직 주행 기록이 없다 */
    val starsThisTrip: Int? = null,
    /** 받을 수 있는 코인. 0이면 "N코인 받기" 버튼을 숨긴다 */
    val claimableCoins: Int = 0,
    /** 안전점수 카드. null이면 주행 기록이 없어 빈 카드 */
    val safetyScore: SafetyScoreChart? = null,
    /** 이번 주행 1km 기록 카드 (2회차) */
    val tripRecord: TripKmRecord? = null,
    /** 상점 바로가기 줄 (2회차) */
    val shop: ShopTeaser? = null,
    /** 받은 기록 (2회차), 최신이 앞 */
    val history: List<CoinHistoryEntry> = emptyList(),
)

/**
 * 안전점수 카드: 이번 주행의 거리축 점수 변화.
 *
 * @param scores 1km마다의 점수. [0]은 출발 때 점수, [k]는 k km 지점 점수
 * @param riskKm 위험 운전이 있었던 km 번호 (1부터). 주황 점으로 표시
 */
data class SafetyScoreChart(
    val scores: List<Float>,
    val riskKm: List<Int>,
) {
    /** 주행 거리 (km) */
    val distanceKm: Int get() = (scores.size - 1).coerceAtLeast(0)
    val startScore: Float get() = scores.firstOrNull() ?: 0f
    val currentScore: Float get() = scores.lastOrNull() ?: 0f
    /** 80점 이상이면 적립 중, 미만이면 적립 멈춤 */
    val isAccruing: Boolean get() = currentScore >= ACCRUAL_THRESHOLD

    companion object {
        /** 적립 기준 점수 (기획안 E-02: 80점 미만이면 다시 오를 때까지 멈춤) */
        const val ACCRUAL_THRESHOLD = 80f
    }
}

/**
 * 이번 주행 1km 기록 카드 (E-03).
 * 이벤트는 급가속·급감속·급정거·과속 4종. 디자인의 "차량 경고"는 ADAS(미지원)라 뺐다.
 *
 * @param cleanCells km마다 깨끗했는지. 크기 = 주행 km
 */
data class TripKmRecord(
    val cleanCells: List<Boolean>,
    val hardAccel: Int,
    val hardBrake: Int,
    val speeding: Int,
    val hardStop: Int,
) {
    val totalKm: Int get() = cleanCells.size
    val cleanKm: Int get() = cleanCells.count { it }
}

/**
 * 상점 바로가기 줄 (2회차). "보관함에 새 친구 1개 · 은하수 비행운까지 216코인"
 *
 * @param newInStorage 보관함에 새로 들어온 아이템 수
 * @param nextItemName 다음 목표 아이템 이름. null이면 목표 문구 없음
 * @param coinsToNext 다음 목표까지 남은 코인
 */
data class ShopTeaser(
    val newInStorage: Int,
    val nextItemName: String?,
    val coinsToNext: Int?,
)

/**
 * 받은 기록 한 줄 (2회차).
 *
 * @param amount 받은 코인은 양수, 쓴 코인은 음수
 */
data class CoinHistoryEntry(
    val title: String,
    val subtitle: String,
    val amount: Int,
)
