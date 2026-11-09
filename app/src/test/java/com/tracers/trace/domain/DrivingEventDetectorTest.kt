package com.tracers.trace.domain

import com.tracers.trace.domain.DrivingEventType.HARD_ACCEL
import com.tracers.trace.domain.DrivingEventType.HARD_DECEL
import com.tracers.trace.domain.DrivingEventType.HARD_STOP
import com.tracers.trace.domain.DrivingEventType.SPEEDING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingEventDetectorTest {

    private fun r(t: Long, kmh: Double) = SpeedReading(t, (kmh / 3.6).toFloat())

    /** (시각 ms, km/h) 쌍을 차례로 넣고 나온 이벤트 종류 */
    private fun DrivingEventDetector.feed(vararg pts: Pair<Long, Double>): List<DrivingEventType> =
        pts.flatMap { (t, v) -> onSpeed(r(t, v)) }.map { it.type }

    /** from부터 to까지 stepMs마다 kmh(t) 값을 넣는다 */
    private fun DrivingEventDetector.ramp(from: Long, to: Long, stepMs: Long = 100, kmh: (Long) -> Double) =
        (from..to step stepMs).flatMap { t -> onSpeed(r(t, kmh(t))) }.map { it.type }

    // ---------- 급가속 ----------

    @Test
    fun `1초에 11kmh 이상 빨라지면 급가속 1회, 계속 가속해도 한 번만 센다`() {
        val d = DrivingEventDetector()
        assertEquals(listOf(HARD_ACCEL), d.feed(0L to 0.0, 1_000L to 12.0, 2_000L to 24.0, 3_000L to 36.0))
    }

    @Test
    fun `가속이 풀렸다가 다시 급가속하면 또 센다, 정확히 기준값도 급가속`() {
        val d = DrivingEventDetector()
        val out = d.feed(0L to 0.0, 1_000L to 12.0, 2_000L to 15.0, 3_000L to 26.0)
        assertEquals(listOf(HARD_ACCEL, HARD_ACCEL), out)
    }

    @Test
    fun `100ms 간격으로 1초에 5kmh 오르는 건 급가속이 아니다`() {
        val d = DrivingEventDetector()
        assertEquals(emptyList<DrivingEventType>(), d.ramp(0, 5_000) { it / 1000.0 * 5 })
    }

    @Test
    fun `차속이 바뀔 때만 와도 1초 전 값과 비교한다 (정차 4초 뒤 0에서 40)`() {
        val d = DrivingEventDetector()
        assertEquals(listOf(HARD_ACCEL), d.feed(0L to 0.0, 4_000L to 0.0, 4_500L to 40.0))
    }

    // ---------- 급감속·급정거 ----------

    @Test
    fun `초당 -7점5 ~ -11은 급감속 1회 (감속이 끝날 때 센다)`() {
        val d = DrivingEventDetector()
        // 100ms마다 -0.9km/h = 초당 -9
        val out = d.ramp(0, 3_000) { t -> 60.0 - (t / 100) * 0.9 } +
            d.ramp(3_100, 5_000) { 33.0 }
        assertEquals(listOf(HARD_DECEL), out)
    }

    @Test
    fun `초당 -11 이상 감속은 급정거로만 센다 (급감속과 중복 없음)`() {
        val d = DrivingEventDetector()
        // 초당 -8 → 점점 세져 초당 -15까지, 그다음 멈춤
        val out = d.feed(0L to 60.0, 1_000L to 52.0, 2_000L to 40.0, 3_000L to 25.0, 4_000L to 10.0, 5_000L to 0.0) +
            d.feed(6_000L to 0.0, 7_000L to 0.0)
        assertEquals(listOf(HARD_STOP), out)
    }

    @Test
    fun `급정거는 기준을 넘는 순간 바로 센다`() {
        val d = DrivingEventDetector()
        assertEquals(listOf(HARD_STOP), d.feed(0L to 50.0, 1_000L to 38.0))
    }

    @Test
    fun `초당 -7 감속은 아무 이벤트도 아니다`() {
        val d = DrivingEventDetector()
        assertEquals(emptyList<DrivingEventType>(), d.feed(0L to 50.0, 1_000L to 43.0, 2_000L to 36.0, 3_000L to 36.0))
    }

    @Test
    fun `튐 거르기를 거친 값은 20→55→19에서 급가속·급정거가 나오지 않는다`() {
        val f = SpeedSpikeFilter()
        val d = DrivingEventDetector()
        val input = listOf(0L to 20.0, 1_000L to 20.0, 1_100L to 55.0, 1_200L to 19.0, 2_200L to 19.0, 3_300L to 19.0)
        val out = input.flatMap { (t, v) -> f.push(r(t, v)) }.flatMap { d.onSpeed(it) }
        assertEquals(emptyList<DrivingEvent>(), out)
    }

    // ---------- 과속 ----------

    @Test
    fun `제한속도 +10 초과가 10초 이어지면 과속 1회`() {
        val d = DrivingEventDetector()
        d.onSpeedLimit(60)
        val out = d.ramp(0, 15_000, stepMs = 500) { 75.0 }
        assertEquals(listOf(SPEEDING), out)
    }

    @Test
    fun `10초가 되기 전에 내려오면 세지 않는다, 딱 +10은 초과가 아니다`() {
        val d = DrivingEventDetector()
        d.onSpeedLimit(60)
        val out = d.ramp(0, 9_000, stepMs = 500) { 75.0 } +
            d.ramp(9_500, 30_000, stepMs = 500) { 70.0 }
        assertEquals(emptyList<DrivingEventType>(), out)
    }

    @Test
    fun `과속이 풀렸다가 다시 10초 이어지면 또 센다`() {
        val d = DrivingEventDetector()
        d.onSpeedLimit(60)
        val out = d.ramp(0, 10_000, stepMs = 500) { 75.0 } +
            d.ramp(10_500, 12_000, stepMs = 500) { 69.0 } +
            d.ramp(12_500, 23_000, stepMs = 500) { 75.0 }
        assertEquals(listOf(SPEEDING, SPEEDING), out)
    }

    @Test
    fun `제한속도 정보가 없으면(null 또는 0) 과속을 세지 않고, 도중에 끊기면 처음부터 다시 잰다`() {
        val d = DrivingEventDetector()
        d.onSpeedLimit(0)
        assertEquals(emptyList<DrivingEventType>(), d.ramp(0, 20_000, stepMs = 500) { 75.0 })

        d.onSpeedLimit(60)
        val first = d.ramp(20_500, 26_000, stepMs = 500) { 75.0 }
        d.onSpeedLimit(null)
        val gap = d.ramp(26_500, 27_000, stepMs = 500) { 75.0 }
        d.onSpeedLimit(60)
        val second = d.ramp(27_500, 36_000, stepMs = 500) { 75.0 }
        assertEquals(emptyList<DrivingEventType>(), first + gap + second)
    }

    // ---------- 거칠었음 (B-04) ----------

    @Test
    fun `급가속·급감속이 최근 2분에 2회면 거칠었음, 2분이 지나면 풀린다`() {
        val w = RoughDrivingWindow()
        w.onEvent(DrivingEvent(HARD_ACCEL, 0))
        assertFalse(w.isRough(1_000))
        w.onEvent(DrivingEvent(HARD_DECEL, 60_000))
        assertTrue(w.isRough(60_000))
        assertTrue(w.isRough(120_000))
        assertFalse(w.isRough(120_001))
    }

    @Test
    fun `과속은 거칠었음에 넣지 않는다, 급정거는 넣는다`() {
        val w = RoughDrivingWindow()
        w.onEvent(DrivingEvent(SPEEDING, 0))
        w.onEvent(DrivingEvent(SPEEDING, 1_000))
        assertFalse(w.isRough(1_000))
        w.onEvent(DrivingEvent(HARD_STOP, 2_000))
        w.onEvent(DrivingEvent(HARD_ACCEL, 3_000))
        assertTrue(w.isRough(3_000))
    }
}
