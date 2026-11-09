package com.tracers.trace.data.record

import androidx.room.withTransaction
import com.tracers.trace.domain.CoinHistoryItem
import com.tracers.trace.domain.CoinHistoryKind
import com.tracers.trace.domain.CoinLedger
import com.tracers.trace.domain.CoinWallet
import com.tracers.trace.domain.EventCounts
import com.tracers.trace.domain.KmResult
import com.tracers.trace.domain.KmSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 1km 기록 한 줄 (domain 쪽 모양) */
data class KmRecord(
    val tripStartedAtMs: Long,
    val segment: KmSegment,
    val kmScore: Int,
    val scoreBefore: Double,
    val scoreAfter: Double,
    val coinsEarned: Int,
)

/**
 * 안전점수·코인 저장소 (E-01~E-04). 계산은 [CoinLedger], 여기서는 읽기 → 계산 → 쓰기를
 * 한 트랜잭션으로 묶기만 한다 (수집과 화면의 받기 버튼이 동시에 지갑을 바꿔도 값이 꼬이지 않게).
 */
class CoinRepository(private val db: TraceDatabase) {

    /** 1km 하나를 안전점수·코인에 반영하고 기록을 남긴다 */
    suspend fun applyKm(tripStartedAtMs: Long, seg: KmSegment): KmResult = db.withTransaction {
        val result = CoinLedger.onKm(loadWallet(), seg.counts)
        db.kmRecordDao().insert(
            KmRecordEntity(
                tripStartedAtMs = tripStartedAtMs, kmIndex = seg.index,
                startedAtMs = seg.startedAtMs, endedAtMs = seg.endedAtMs,
                hardAccel = seg.counts.hardAccel, hardDecel = seg.counts.hardDecel,
                hardStop = seg.counts.hardStop, speeding = seg.counts.speeding,
                kmScore = result.kmScore, scoreBefore = result.scoreBefore, scoreAfter = result.scoreAfter,
                coinsEarned = result.coinsEarned,
            )
        )
        saveWallet(result.wallet)
        result
    }

    /** 받을 코인을 모두 받는다. 받은 개수 (없으면 0) */
    suspend fun claim(nowMs: Long): Int = db.withTransaction {
        val (wallet, entry) = CoinLedger.claim(loadWallet(), nowMs)
        if (entry == null) return@withTransaction 0
        saveWallet(wallet)
        addHistory(entry)
        entry.amount
    }

    /** 보유 코인을 쓴다 (상점). 모자라면 false */
    suspend fun spend(amount: Int, note: String, nowMs: Long): Boolean = db.withTransaction {
        val (wallet, entry) = CoinLedger.spend(loadWallet(), amount, note, nowMs) ?: return@withTransaction false
        saveWallet(wallet)
        addHistory(entry)
        true
    }

    fun observeWallet(): Flow<CoinWallet> = db.coinWalletDao().observe().map { it?.toDomain() ?: CoinWallet() }

    /** 최근 코인 기록, 최신이 앞 */
    fun observeHistory(): Flow<List<CoinHistoryItem>> =
        db.coinHistoryDao().observeRecent(CoinLedger.HISTORY_LIMIT).map { list ->
            list.map { CoinHistoryItem(it.atMs, CoinHistoryKind.valueOf(it.kind), it.amount, it.note) }
        }

    /** 가장 최근 주행의 1km 기록, km 순 */
    fun observeLatestTripKms(): Flow<List<KmRecord>> =
        db.kmRecordDao().observeLatestTrip().map { list ->
            list.map {
                KmRecord(
                    tripStartedAtMs = it.tripStartedAtMs,
                    segment = KmSegment(
                        index = it.kmIndex, startedAtMs = it.startedAtMs, endedAtMs = it.endedAtMs,
                        counts = EventCounts(it.hardAccel, it.hardDecel, it.hardStop, it.speeding),
                    ),
                    kmScore = it.kmScore, scoreBefore = it.scoreBefore, scoreAfter = it.scoreAfter,
                    coinsEarned = it.coinsEarned,
                )
            }
        }

    /** 보관 기간(기본 100일)이 지난 1km 기록 삭제 */
    suspend fun prune(nowMs: Long, keepDays: Int = 100) {
        db.kmRecordDao().deleteBefore(nowMs - keepDays * 86_400_000L)
    }

    private suspend fun loadWallet(): CoinWallet = db.coinWalletDao().get()?.toDomain() ?: CoinWallet()

    private suspend fun saveWallet(w: CoinWallet) =
        db.coinWalletDao().upsert(CoinWalletEntity(safetyScore = w.safetyScore, unclaimedCoins = w.unclaimed, balanceCoins = w.balance))

    private suspend fun addHistory(e: CoinHistoryItem) {
        db.coinHistoryDao().insert(CoinHistoryEntity(atMs = e.atMs, kind = e.kind.name, amount = e.amount, note = e.note))
        db.coinHistoryDao().trimTo(CoinLedger.HISTORY_LIMIT)
    }

    private fun CoinWalletEntity.toDomain() = CoinWallet(safetyScore, unclaimedCoins, balanceCoins)
}
