package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoinLedgerTest {

    private val clean = EventCounts()
    private val risky = EventCounts(hardAccel = 1)

    @Test
    fun `처음 지갑은 안전점수 85, 코인 0`() {
        assertEquals(CoinWallet(safetyScore = 85.0, unclaimed = 0, balance = 0), CoinWallet())
    }

    @Test
    fun `80점 이상에서 깨끗한 1km는 1코인을 받을 코인에 쌓고 점수를 올린다`() {
        val r = CoinLedger.onKm(CoinWallet(), clean)
        assertEquals(1, r.coinsEarned)
        assertEquals(100, r.kmScore)
        assertEquals(85.0, r.scoreBefore, 0.0)
        assertEquals(85.3, r.scoreAfter, 1e-9)
        assertEquals(CoinWallet(safetyScore = 85.3, unclaimed = 1, balance = 0), r.wallet)
    }

    @Test
    fun `이벤트가 있는 1km는 코인이 없고 점수가 내려간다`() {
        val r = CoinLedger.onKm(CoinWallet(), risky)
        assertEquals(0, r.coinsEarned)
        assertEquals(80, r.kmScore)
        assertEquals(84.9, r.wallet.safetyScore, 1e-9)
        assertEquals(0, r.wallet.unclaimed)
    }

    @Test
    fun `80점 미만이면 깨끗하게 달려도 적립을 멈추고 점수만 오른다`() {
        val r = CoinLedger.onKm(CoinWallet(safetyScore = 79.9), clean)
        assertEquals(0, r.coinsEarned)
        assertEquals(79.9 * 0.98 + 2.0, r.wallet.safetyScore, 1e-9)
    }

    @Test
    fun `적립 여부는 그 1km를 달릴 때의 점수(반영 전)로 정한다, 정확히 80은 적립`() {
        assertEquals(1, CoinLedger.onKm(CoinWallet(safetyScore = 80.0), clean).coinsEarned)
        // 79.7 → 깨끗한 1km 뒤 80.106이 되지만 이번 1km는 적립 안 함
        val r = CoinLedger.onKm(CoinWallet(safetyScore = 79.7), clean)
        assertEquals(0, r.coinsEarned)
        assertEquals(1, CoinLedger.onKm(r.wallet, clean).coinsEarned)
    }

    @Test
    fun `받기는 받을 코인을 보유로 옮기고 기록 한 줄을 남긴다`() {
        val (w, entry) = CoinLedger.claim(CoinWallet(unclaimed = 37, balance = 100), nowMs = 5_000)
        assertEquals(CoinWallet(unclaimed = 0, balance = 137), w)
        assertEquals(CoinHistoryItem(atMs = 5_000, kind = CoinHistoryKind.EARN, amount = 37, note = null), entry)
    }

    @Test
    fun `받을 코인이 없으면 받기는 아무것도 바꾸지 않는다`() {
        val w0 = CoinWallet(balance = 10)
        val (w, entry) = CoinLedger.claim(w0, nowMs = 0)
        assertEquals(w0, w)
        assertNull(entry)
    }

    @Test
    fun `쓰기는 보유에서 빼고 음수 기록을 남긴다, 모자라면 null`() {
        val (w, entry) = CoinLedger.spend(CoinWallet(balance = 300), amount = 216, note = "milky_way", nowMs = 9)!!
        assertEquals(84, w.balance)
        assertEquals(CoinHistoryItem(9, CoinHistoryKind.SPEND, -216, "milky_way"), entry)
        assertNull(CoinLedger.spend(CoinWallet(balance = 100), amount = 216, note = "x", nowMs = 9))
        assertNull(CoinLedger.spend(CoinWallet(balance = 100), amount = 0, note = "x", nowMs = 9))
    }
}
