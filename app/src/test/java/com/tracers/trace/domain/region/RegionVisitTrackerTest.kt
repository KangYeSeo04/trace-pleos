package com.tracers.trace.domain.region

import com.tracers.trace.domain.SpeedReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionVisitTrackerTest {

    private val t = RegionVisitTracker()

    /** [fromMs]부터 [toMs]까지 100ms마다 같은 차속 */
    private fun drive(mps: Float, fromMs: Long, toMs: Long) {
        var at = fromMs
        while (at <= toMs) { t.onSpeed(SpeedReading(at, mps)); at += 100 }
    }

    private fun List<RegionVisitDelta>.of(code: String) = single { it.regionCode == code }

    @Test fun firstFixCreatesVisitWithNoDistance() {
        t.onRegion("41130", 1_000)
        val d = t.drain().of("41130")
        assertEquals(0.0, d.distanceKm, 0.0)
        assertEquals(0L, d.stayMs)
        assertEquals(1_000L, d.atMs)
    }

    @Test fun distanceIsIntegratedSpeedInCurrentRegion() {
        t.onRegion("41130", 0)
        drive(10f, 0, 60_000)                  // 10 m/s × 60 s = 600 m
        assertEquals(0.6, t.drain().of("41130").distanceKm, 1e-6)
    }

    @Test fun distanceSplitsAtBorder() {
        t.onRegion("41130", 0)
        drive(20f, 0, 50_000)                  // 1 km 성남
        t.onRegion("11000", 50_000)
        drive(20f, 50_100, 100_000)            // 약 1 km 서울
        val out = t.drain()
        assertEquals(1.0, out.of("41130").distanceKm, 1e-6)
        assertEquals(1.0, out.of("11000").distanceKm, 1e-6)
    }

    @Test fun speedBeforeFirstFixIsDropped() {
        drive(20f, 0, 10_000)
        t.onRegion("11000", 10_000)
        drive(20f, 10_100, 20_000)
        assertEquals(0.2, t.drain().of("11000").distanceKm, 1e-6)
    }

    @Test fun longSpeedGapIsNotIntegrated() {
        t.onRegion("11000", 0)
        t.onSpeed(SpeedReading(0, 30f))
        t.onSpeed(SpeedReading(60_000, 30f))   // 1분 끊김: 넣지 않음
        assertEquals(0.0, t.drain().of("11000").distanceKm, 0.0)
    }

    @Test fun stayIsTimeBetweenFixesInSameRegion() {
        t.onRegion("11000", 0)
        t.onRegion("11000", 60_000)
        t.onRegion("11000", 120_000)
        assertEquals(120_000L, t.drain().of("11000").stayMs)
    }

    @Test fun stayUntilBorderGoesToPreviousRegion() {
        t.onRegion("41130", 0)
        t.onRegion("11000", 5_000)
        t.onRegion("11000", 10_000)
        val out = t.drain()
        assertEquals(5_000L, out.of("41130").stayMs)
        assertEquals(5_000L, out.of("11000").stayMs)
    }

    @Test fun longFixGapIsNotStay() {
        t.onRegion("11000", 0)
        t.onRegion("11000", RegionVisitTracker.MAX_STAY_GAP_MS + 1)  // 앱이 꺼져 있었거나 위치가 끊김
        assertEquals(0L, t.drain().of("11000").stayMs)
    }

    @Test fun unknownRegionStopsAccumulation() {
        t.onRegion("11000", 0)
        t.onRegion(null, 5_000)                // 위치 못 받음
        drive(20f, 5_000, 20_000)
        t.onRegion(null, 10_000)
        val d = t.drain().of("11000")
        assertEquals(5_000L, d.stayMs)
        assertEquals(0.0, d.distanceKm, 0.0)
    }

    @Test fun drainEmptiesPending() {
        t.onRegion("11000", 0)
        drive(10f, 0, 10_000)
        t.drain()
        assertTrue(t.drain().isEmpty())
        drive(10f, 10_100, 20_000)
        assertEquals(0.1, t.drain().of("11000").distanceKm, 1e-6)
    }

    @Test fun resetForgetsRegionAndPending() {
        t.onRegion("11000", 0)
        drive(10f, 0, 10_000)
        t.reset()
        assertTrue(t.drain().isEmpty())
        drive(10f, 10_100, 20_000)
        assertTrue(t.drain().isEmpty())
        assertEquals(null, t.currentRegion)
    }

    @Test fun discardDropsOnlyThatRegionsPending() {
        t.onRegion("41130", 0)
        drive(20f, 0, 10_000)
        t.onRegion("11000", 10_000)
        drive(20f, 10_100, 20_000)
        t.discard("41130")
        val out = t.drain()
        assertEquals(listOf("11000"), out.map { it.regionCode })
    }
}
