package com.tracers.trace.domain

import com.tracers.trace.domain.DrivingEventType.HARD_ACCEL
import com.tracers.trace.domain.DrivingEventType.HARD_STOP
import com.tracers.trace.domain.DrivingEventType.SPEEDING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KmSegmenterTest {

    private fun r(t: Long, kmh: Double) = SpeedReading(t, (kmh / 3.6).toFloat())

    /** 36km/h(10m/s)로 from~to ms를 100ms마다 넣고 나온 구간들 */
    private fun KmSegmenter.drive(from: Long, to: Long, kmh: Double = 36.0) =
        (from..to step 100).mapNotNull { onSpeed(r(it, kmh)) }

    @Test
    fun `차속을 적분해 1km마다 구간 하나를 낸다`() {
        val s = KmSegmenter()
        // 10m/s × 250초 = 2.5km → 구간 2개, 남은 0.5km는 아직
        val segs = s.drive(0, 250_000)
        assertEquals(2, segs.size)
        assertEquals(1, segs[0].index)
        assertEquals(2, segs[1].index)
        assertEquals(0L, segs[0].startedAtMs)
        assertEquals(100_000.0, segs[0].endedAtMs.toDouble(), 200.0)
        assertEquals(segs[0].endedAtMs, segs[1].startedAtMs)
    }

    @Test
    fun `이벤트는 그때 달리던 구간에 붙는다`() {
        val s = KmSegmenter()
        s.drive(0, 50_000)
        s.onEvent(DrivingEvent(HARD_ACCEL, 50_000))
        s.onEvent(DrivingEvent(SPEEDING, 60_000))
        val first = s.drive(50_100, 100_100).single()
        s.onEvent(DrivingEvent(HARD_STOP, 120_000))
        val second = s.drive(100_200, 200_200).single()

        assertEquals(EventCounts(hardAccel = 1, speeding = 1), first.counts)
        assertFalse(first.counts.isClean)
        assertEquals(EventCounts(hardStop = 1), second.counts)
    }

    @Test
    fun `이벤트 없는 구간은 깨끗한 1km`() {
        val seg = KmSegmenter().drive(0, 101_000).single()
        assertTrue(seg.counts.isClean)
    }

    @Test
    fun `신호가 5초 넘게 끊긴 간격은 거리에 넣지 않는다`() {
        val s = KmSegmenter()
        s.drive(0, 50_000)
        // 60초 공백 뒤 이어서: 공백을 넣으면 0.6km가 더해져 1km를 넘는다
        assertEquals(null, s.onSpeed(r(110_000, 36.0)))
        val seg = s.drive(110_100, 160_100).single()
        assertEquals(1, seg.index)
    }

    @Test
    fun `reset하면 덜 찬 구간과 번호를 버리고 처음부터 센다`() {
        val s = KmSegmenter()
        s.drive(0, 90_000)
        s.onEvent(DrivingEvent(HARD_ACCEL, 90_000))
        s.reset()
        val seg = s.drive(200_000, 300_100).single()
        assertEquals(1, seg.index)
        assertEquals(200_000L, seg.startedAtMs)
        assertTrue(seg.counts.isClean)
    }

    @Test
    fun `정차 중(속도 0)에는 거리가 늘지 않는다`() {
        val s = KmSegmenter()
        s.drive(0, 60_000, kmh = 0.0)
        assertEquals(emptyList<KmSegment>(), s.drive(60_100, 99_000))
    }
}
