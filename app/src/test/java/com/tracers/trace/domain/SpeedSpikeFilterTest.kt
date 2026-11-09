package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedSpikeFilterTest {

    private fun r(t: Long, kmh: Double) = SpeedReading(t, (kmh / 3.6).toFloat())

    /** 넣은 순서대로 통과한 값의 km/h (소수 1자리 반올림) */
    private fun SpeedSpikeFilter.run(vararg input: SpeedReading): List<Pair<Long, Double>> =
        (input.flatMap { push(it) } + flush()).map { it.timeMs to Math.round(it.kmh * 10) / 10.0 }

    @Test
    fun `300ms 안에 튀었다 돌아오는 값 하나는 버린다 (20→55→19)`() {
        val out = SpeedSpikeFilter().run(r(0, 20.0), r(100, 55.0), r(200, 19.0), r(300, 19.0))
        assertEquals(listOf(0L to 20.0, 200L to 19.0, 300L to 19.0), out)
    }

    @Test
    fun `아래로 튀는 값도 버린다 (60→0→60)`() {
        val out = SpeedSpikeFilter().run(r(0, 60.0), r(100, 0.0), r(200, 60.0))
        assertEquals(listOf(0L to 60.0, 200L to 60.0), out)
    }

    @Test
    fun `튄 뒤 300ms가 지나서 돌아오면 진짜 값으로 본다`() {
        val out = SpeedSpikeFilter().run(r(0, 20.0), r(100, 55.0), r(500, 20.0))
        assertEquals(listOf(0L to 20.0, 100L to 55.0, 500L to 20.0), out)
    }

    @Test
    fun `크게 바뀐 뒤 그 값에 머물면 진짜 변화다 (정차 0에서 40)`() {
        val out = SpeedSpikeFilter().run(r(0, 0.0), r(1_000, 40.0), r(1_100, 40.0))
        assertEquals(listOf(0L to 0.0, 1_000L to 40.0, 1_100L to 40.0), out)
    }

    @Test
    fun `작은 흔들림과 꾸준한 가속은 그대로 통과한다`() {
        val out = SpeedSpikeFilter().run(r(0, 30.0), r(100, 31.0), r(200, 30.5), r(300, 32.0), r(400, 33.0))
        assertEquals(listOf(0L to 30.0, 100L to 31.0, 200L to 30.5, 300L to 32.0, 400L to 33.0), out)
    }

    @Test
    fun `튄 값 다음 값이 또 튀어도 하나씩만 버린다`() {
        // 20 → 55(튐) → 19 → 80(튐) → 21
        val out = SpeedSpikeFilter().run(r(0, 20.0), r(100, 55.0), r(200, 19.0), r(300, 80.0), r(400, 21.0))
        assertEquals(listOf(0L to 20.0, 200L to 19.0, 400L to 21.0), out)
    }

    @Test
    fun `값 하나 차이로 반쯤만 돌아오면 튐이 아니다 (20→55→45)`() {
        val out = SpeedSpikeFilter().run(r(0, 20.0), r(100, 55.0), r(200, 45.0), r(300, 45.0))
        assertEquals(listOf(0L to 20.0, 100L to 55.0, 200L to 45.0, 300L to 45.0), out)
    }
}
