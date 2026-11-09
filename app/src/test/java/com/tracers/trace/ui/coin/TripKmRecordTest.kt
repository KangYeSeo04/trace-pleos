package com.tracers.trace.ui.coin

import com.tracers.trace.ui.screens.coin.TripKmRecord
import com.tracers.trace.ui.screens.coin.stripDescription
import org.junit.Assert.assertEquals
import org.junit.Test

/** 1km 기록 가로 막대: 멈춘 km를 이어진 구간으로 묶는지 (CoinDark/Light runs 계산과 같게) */
class TripKmRecordTest {

    private fun record(vararg clean: Boolean) = TripKmRecord(clean.toList(), 0, 0, 0, 0)

    @Test
    fun 모두_깨끗하면_구간_없음() = assertEquals(emptyList<IntRange>(), record(true, true, true).stoppedRuns)

    @Test
    fun 이어진_km는_하나로_묶고_km는_1부터() {
        // 1 깨끗, 2·3 멈춤, 4 깨끗, 5 멈춤
        assertEquals(listOf(2..3, 5..5), record(true, false, false, true, false).stoppedRuns)
    }

    @Test
    fun 처음과_끝이_멈춤() = assertEquals(listOf(1..1, 3..4), record(false, true, false, false).stoppedRuns)

    @Test
    fun 주행_없으면_구간_없음() = assertEquals(emptyList<IntRange>(), record().stoppedRuns)

    @Test
    fun 화면_읽기_문구() {
        assertEquals("37 km 중 7 km, 12–14 km에서 적립이 멈췄어요", stripDescription(37, listOf(7..7, 12..14)))
        assertEquals("5 km 모두 적립했어요", stripDescription(5, emptyList()))
    }
}
