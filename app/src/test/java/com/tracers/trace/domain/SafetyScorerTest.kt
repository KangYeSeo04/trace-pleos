package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyScorerTest {

    @Test
    fun `이벤트 없는 1km는 100점`() {
        assertEquals(100, SafetyScorer.kmScore(EventCounts()))
    }

    @Test
    fun `급가속·급감속·과속 -20, 급정거 -30`() {
        assertEquals(80, SafetyScorer.kmScore(EventCounts(hardAccel = 1)))
        assertEquals(80, SafetyScorer.kmScore(EventCounts(hardDecel = 1)))
        assertEquals(70, SafetyScorer.kmScore(EventCounts(hardStop = 1)))
        assertEquals(80, SafetyScorer.kmScore(EventCounts(speeding = 1)))
        assertEquals(30, SafetyScorer.kmScore(EventCounts(hardAccel = 1, hardDecel = 1, hardStop = 1)))
    }

    @Test
    fun `1km 점수는 0 아래로 내려가지 않는다`() {
        assertEquals(0, SafetyScorer.kmScore(EventCounts(hardStop = 3, hardAccel = 2)))
    }

    @Test
    fun `안전점수 = 기존 × 0_98 + 이번 1km × 0_02, 시작 85`() {
        assertEquals(85.0, SafetyScorer.START, 0.0)
        assertEquals(85.3, SafetyScorer.next(85.0, 100), 1e-9)
        assertEquals(83.3, SafetyScorer.next(85.0, 0), 1e-9)
        assertEquals(84.7, SafetyScorer.next(85.0, 70), 1e-9)
    }
}
