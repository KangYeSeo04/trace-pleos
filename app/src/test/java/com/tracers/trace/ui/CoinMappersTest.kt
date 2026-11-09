package com.tracers.trace.ui

import com.tracers.trace.domain.CoinHistoryItem
import com.tracers.trace.domain.CoinHistoryKind
import com.tracers.trace.domain.CoinWallet
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.EventCounts
import com.tracers.trace.domain.KmRecord
import com.tracers.trace.domain.KmSegment
import com.tracers.trace.ui.screens.coin.CoinHistoryEntry
import com.tracers.trace.ui.screens.coin.CoinUiMapper
import com.tracers.trace.ui.screens.home.CoinTileMapper
import com.tracers.trace.ui.screens.home.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class CoinMappersTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun km(i: Int, counts: EventCounts = EventCounts(), before: Double, after: Double, coins: Int) =
        KmRecord(0, KmSegment(i, i * 60_000L, (i + 1) * 60_000L, counts), 100, before, after, coins)

    private val trip = listOf(
        km(1, before = 85.0, after = 85.3, coins = 1),
        km(2, EventCounts(hardAccel = 1, hardStop = 1), before = 85.3, after = 84.99, coins = 0),
        km(3, EventCounts(hardDecel = 1, speeding = 1), before = 84.99, after = 84.39, coins = 0),
        km(4, before = 84.39, after = 84.7, coins = 1),
    )

    @Test
    fun `코인 화면 - 별은 깨끗한 km 수, 꺾은선은 출발 점수부터, 위험 km와 이벤트 4종 합`() {
        val s = CoinUiMapper.map(CoinWallet(84.7, unclaimed = 2, balance = 10), trip, emptyList(), parked = true, zone = seoul)
        assertEquals(10, s.balance)
        assertEquals(2, s.starsThisTrip)
        assertEquals(2, s.claimableCoins)
        assertEquals(listOf(85.0f, 85.3f, 84.99f, 84.39f, 84.7f), s.safetyScore!!.scores)
        assertEquals(listOf(2, 3), s.safetyScore!!.riskKm)
        val r = s.tripRecord!!
        assertEquals(listOf(true, false, false, true), r.cleanCells)
        assertEquals(listOf(1, 1, 1, 1), listOf(r.hardAccel, r.hardBrake, r.hardStop, r.speeding))
    }

    @Test
    fun `받기 버튼은 주차 중에만, 주행 기록이 없으면 빈 카드`() {
        val s = CoinUiMapper.map(CoinWallet(unclaimed = 5), emptyList(), emptyList(), parked = false, zone = seoul)
        assertEquals(0, s.claimableCoins)
        assertNull(s.starsThisTrip)
        assertNull(s.safetyScore)
        assertNull(s.tripRecord)
    }

    @Test
    fun `받은 기록은 날짜와 +N, 쓴 기록은 -N`() {
        val nov5 = 1_793_836_800_000L // 2026-11-05 00:00 KST 무렵
        val h = listOf(
            CoinHistoryItem(nov5, CoinHistoryKind.EARN, 31, null),
            CoinHistoryItem(nov5, CoinHistoryKind.SPEND, -500, "stardust"),
        )
        val s = CoinUiMapper.map(CoinWallet(), emptyList(), h, parked = true, zone = seoul)
        assertEquals(CoinHistoryEntry("11월 5일", "깨끗한 31 km 적립", 31), s.history[0])
        assertEquals(-500, s.history[1].amount)
    }

    @Test
    fun `홈 타일 - 보유 코인, 주차 후 이번 주행 적립, 안전점수 막대(80 = 눈금 50%)`() {
        val t = CoinTileMapper.map(CoinWallet(85.3, unclaimed = 2, balance = 1284), trip, DriveState.PARKED)
        assertEquals("1,284", t.value)
        assertEquals("이번 주행 +2", t.status)
        assertEquals(63.25f, t.barPercent, 1e-3f)
        assertEquals(50f, t.barMarkPercent!!, 1e-3f)
        assertEquals("안전점수 85.3 · 적립 기준 80", t.subText)
    }

    @Test
    fun `홈 타일 - 주행 중에는 적립 수를 보이지 않고, 80 미만이면 적립 멈춤(주의색)`() {
        assertEquals("별 모으는 중", CoinTileMapper.map(CoinWallet(), trip, DriveState.DRIVING).status)
        val low = CoinTileMapper.map(CoinWallet(79.0), emptyList(), DriveState.PARKED)
        assertEquals(StatusLevel.Caution, low.statusLevel)
        assertEquals("적립 멈춤 · 80점부터 다시", low.status)
    }
}
