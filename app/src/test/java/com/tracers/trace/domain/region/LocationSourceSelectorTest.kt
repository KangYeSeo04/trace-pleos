package com.tracers.trace.domain.region

import com.tracers.trace.domain.region.LocationSourceSelector.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSourceSelectorTest {

    private val pangyo = GeoFix(37.3947, 127.1112, 0)
    private val gangnam = GeoFix(37.4979, 127.0276, 0)

    /** 5초마다 navi는 같은 자리를 답하고, 차속은 [speed]로 [seconds]초 동안 */
    private fun LocationSourceSelector.drive(startMs: Long, seconds: Int, speed: Float, navi: GeoFix? = pangyo): Long {
        var t = startMs
        onTick(t, speed)
        repeat(seconds / 5) {
            t += 5_000
            onTick(t, speed)
            navi?.let { onNavi(it.copy(timeMs = t)) }
        }
        return t
    }

    @Test
    fun `처음엔 NaviHelper`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        assertEquals(Source.NAVI, s.source)
        assertTrue(s.onNavi(pangyo))
        assertFalse(s.onFused(gangnam))
    }

    @Test
    fun `움직이는데 navi가 1분 넘게 그대로면 FusedLocation으로`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        val t = s.drive(0, 60, speed = 8f)
        assertEquals("정확히 1분까지는 그대로", Source.NAVI, s.source)
        s.onTick(t + 5_000, 8f)
        assertEquals(Source.FUSED, s.source)
        assertFalse("전환 뒤 navi 값은 쓰지 않는다", s.onNavi(pangyo))
        assertTrue(s.onFused(gangnam))
    }

    @Test
    fun `정차 중에는 navi가 그대로여도 전환하지 않는다`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        s.drive(0, 600, speed = 0f)
        assertEquals(Source.NAVI, s.source)
    }

    @Test
    fun `navi가 움직이면 다시 센다`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        var t = s.drive(0, 50, speed = 8f)
        t += 5_000; s.onTick(t, 8f); s.onNavi(gangnam.copy(timeMs = t))
        s.drive(t, 50, speed = 8f, navi = gangnam)
        assertEquals(Source.NAVI, s.source)
    }

    @Test
    fun `navi 답이 아예 없어도 움직이면 전환`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.drive(0, 65, speed = 8f, navi = null)
        assertEquals(Source.FUSED, s.source)
    }

    @Test
    fun `FusedLocation 중 navi가 움직이면 NaviHelper로 돌아온다`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        val t = s.drive(0, 65, speed = 8f)
        assertEquals(Source.FUSED, s.source)
        assertTrue(s.onNavi(gangnam.copy(timeMs = t + 5_000)))
        assertEquals(Source.NAVI, s.source)
    }

    @Test
    fun `FusedLocation을 못 쓰면 NaviHelper만`() {
        val s = LocationSourceSelector(fusedAvailable = false)
        s.onNavi(pangyo)
        s.drive(0, 300, speed = 8f)
        assertEquals(Source.NAVI, s.source)
    }

    @Test
    fun `긴 공백은 움직인 시간으로 세지 않는다`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        s.onTick(0, 8f)
        s.onTick(10 * 60_000L, 8f) // 앱이 멈췄다 깨어남
        assertEquals(Source.NAVI, s.source)
    }

    @Test
    fun `잘못된 navi 값은 움직임으로 보지 않는다`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        var t = 0L
        s.onTick(t, 8f)
        repeat(13) {
            t += 5_000
            s.onTick(t, 8f)
            s.onNavi(GeoFix(0.0, 0.0, t))
        }
        assertEquals(Source.FUSED, s.source)
    }

    @Test
    fun `전환할 때 알린다`() {
        val changes = mutableListOf<Source>()
        val s = LocationSourceSelector(fusedAvailable = true, onSwitch = { changes += it })
        s.onNavi(pangyo)
        val t = s.drive(0, 65, speed = 8f)
        s.onNavi(gangnam.copy(timeMs = t + 5_000))
        assertEquals(listOf(Source.FUSED, Source.NAVI), changes)
    }

    @Test
    fun `정차 중 마지막 간격은 움직인 시간으로 세지 않는다`() {
        val s = LocationSourceSelector(fusedAvailable = true)
        s.onNavi(pangyo)
        s.onTick(0, 0f)
        s.onTick(60_000, 8f) // 정차 60초 간격 뒤 출발
        s.drive(60_000, 55, speed = 8f)
        assertEquals(Source.NAVI, s.source)
    }
}
