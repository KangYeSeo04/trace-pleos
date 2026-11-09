package com.tracers.trace.domain

/**
 * 안전점수와 코인 (한 줄로 저장).
 *
 * @property unclaimed 적립됐지만 아직 받지 않은 코인 ("N코인 받기")
 * @property balance 보유 코인
 */
data class CoinWallet(
    val safetyScore: Double = SafetyScorer.START,
    val unclaimed: Int = 0,
    val balance: Int = 0,
)

enum class CoinHistoryKind { EARN, SPEND }

/**
 * 코인 기록 한 줄 (E-04 받은·쓴 기록).
 *
 * @property amount 받은 코인은 양수, 쓴 코인은 음수
 * @property note 쓴 경우 아이템 id. 받은 경우 null
 */
data class CoinHistoryItem(
    val atMs: Long,
    val kind: CoinHistoryKind,
    val amount: Int,
    val note: String?,
)

/** 1km 하나를 반영한 결과 */
data class KmResult(
    val kmScore: Int,
    val scoreBefore: Double,
    val scoreAfter: Double,
    val coinsEarned: Int,
    val wallet: CoinWallet,
)

/**
 * 코인 적립·받기·쓰기 (E-02, E-04). 지갑을 받아 새 지갑을 돌려주는 순수 계산만 한다. 저장은 data 계층.
 *
 * 적립: 그 1km를 달릴 때의 안전점수(반영 전)가 [DrivingThresholds.COIN_ACCRUAL_MIN_SCORE] 이상이고
 * 깨끗한 1km면 1코인. 80 미만이면 점수가 다시 오를 때까지 멈춘다.
 */
object CoinLedger {
    /** 기록은 최근 이만큼만 남긴다 */
    const val HISTORY_LIMIT = 30

    fun onKm(wallet: CoinWallet, counts: EventCounts): KmResult {
        val km = SafetyScorer.kmScore(counts)
        val before = wallet.safetyScore
        val after = SafetyScorer.next(before, km)
        val earned = if (counts.isClean && before >= DrivingThresholds.COIN_ACCRUAL_MIN_SCORE - EPSILON) {
            DrivingThresholds.COINS_PER_CLEAN_KM
        } else 0
        return KmResult(
            kmScore = km, scoreBefore = before, scoreAfter = after, coinsEarned = earned,
            wallet = wallet.copy(safetyScore = after, unclaimed = wallet.unclaimed + earned),
        )
    }

    /** 받을 코인을 모두 보유로 옮긴다. 받을 코인이 없으면 기록 없이 그대로 */
    fun claim(wallet: CoinWallet, nowMs: Long): Pair<CoinWallet, CoinHistoryItem?> {
        if (wallet.unclaimed <= 0) return wallet to null
        val n = wallet.unclaimed
        return wallet.copy(unclaimed = 0, balance = wallet.balance + n) to
            CoinHistoryItem(nowMs, CoinHistoryKind.EARN, n, null)
    }

    /** 보유에서 쓴다 (상점). 모자라거나 0 이하면 null */
    fun spend(wallet: CoinWallet, amount: Int, note: String, nowMs: Long): Pair<CoinWallet, CoinHistoryItem>? {
        if (amount <= 0 || wallet.balance < amount) return null
        return wallet.copy(balance = wallet.balance - amount) to
            CoinHistoryItem(nowMs, CoinHistoryKind.SPEND, -amount, note)
    }

    /** 점수가 Double 누적이라 80.0이 79.99999로 나올 수 있다 */
    private const val EPSILON = 1e-9
}
