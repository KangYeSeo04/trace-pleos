package com.tracers.trace.ui.screens.coin

import com.tracers.trace.domain.KmRecord
import com.tracers.trace.domain.CoinHistoryItem
import com.tracers.trace.domain.CoinHistoryKind
import com.tracers.trace.domain.CoinWallet
import java.time.Instant
import java.time.ZoneId

/**
 * 지갑·1km 기록·코인 기록 → 코인 화면 상태. 순수 함수.
 *
 * - 별 = 이번(가장 최근) 주행의 깨끗한 1km 수. 위험 구간은 흐린 별
 * - "N코인 받기"는 주차(P) 중에만 (E-02 화면에는 주차 후에만 표시)
 */
object CoinUiMapper {

    fun map(
        wallet: CoinWallet,
        latestTripKms: List<KmRecord>,
        history: List<CoinHistoryItem>,
        parked: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CoinUiState {
        val kms = latestTripKms.sortedBy { it.segment.index }
        return CoinUiState(
            balance = wallet.balance,
            starsThisTrip = if (kms.isEmpty()) null else kms.count { it.segment.counts.isClean },
            claimableCoins = if (parked) wallet.unclaimed else 0,
            safetyScore = chart(kms),
            tripRecord = record(kms),
            shop = null, // TODO(F-01): 상점 아이템 목록이 생기면 다음 목표
            history = history.take(CoinUiState.HISTORY_LIMIT).map { entry(it, zone) },
        )
    }

    private fun chart(kms: List<KmRecord>): SafetyScoreChart? {
        if (kms.isEmpty()) return null
        val scores = listOf(kms.first().scoreBefore) + kms.map { it.scoreAfter }
        return SafetyScoreChart(
            scores = scores.map { it.toFloat() },
            riskKm = kms.filter { !it.segment.counts.isClean }.map { it.segment.index },
        )
    }

    private fun record(kms: List<KmRecord>): TripKmRecord? {
        if (kms.isEmpty()) return null
        val c = kms.map { it.segment.counts }
        return TripKmRecord(
            cleanCells = c.map { it.isClean },
            hardAccel = c.sumOf { it.hardAccel },
            hardBrake = c.sumOf { it.hardDecel },
            speeding = c.sumOf { it.speeding },
            hardStop = c.sumOf { it.hardStop },
        )
    }

    private fun entry(h: CoinHistoryItem, zone: ZoneId): CoinHistoryEntry {
        val d = Instant.ofEpochMilli(h.atMs).atZone(zone)
        val date = "${d.monthValue}월 ${d.dayOfMonth}일"
        return when (h.kind) {
            CoinHistoryKind.EARN -> CoinHistoryEntry(date, "깨끗한 ${h.amount} km 적립", h.amount)
            // TODO(F-01): 아이템 id(note) → 이름
            CoinHistoryKind.SPEND -> CoinHistoryEntry(date, "꾸미기 구매", h.amount)
        }
    }
}
