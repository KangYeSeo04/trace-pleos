package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChargeDetectorTest {

    private fun s(t: Long, port: Boolean? = null, pct: Int? = null, kwh: Double? = null) =
        VehicleSample(timeMs = t, chargePortOpen = port, batteryPercent = pct, batteryKwh = kwh)

    @Test
    fun `충전구 열림 후 배터리 %가 오르고 닫히면 충전 1회`() {
        val d = ChargeDetector()
        assertNull(d.onSample(s(0, port = false, pct = 30, kwh = 23.0)))
        assertNull(d.onSample(s(1_000, port = true)))
        assertNull(d.onSample(s(600_000, pct = 50, kwh = 38.5)))
        assertNull(d.onSample(s(1_200_000, pct = 80, kwh = 61.9)))
        val c = d.onSample(s(1_300_000, port = false))

        assertNotNull(c)
        c!!
        assertEquals(1_000L, c.startedAtMs)
        assertEquals(1_300_000L, c.endedAtMs)
        assertEquals(30, c.startPercent)
        assertEquals(80, c.endPercent)
        assertEquals(38.9, c.addedKwh, 1e-6)
    }

    @Test
    fun `충전구만 열었다 닫고 %가 그대로면 충전이 아니다`() {
        val d = ChargeDetector()
        d.onSample(s(0, port = false, pct = 40))
        d.onSample(s(1_000, port = true))
        assertNull(d.onSample(s(60_000, port = false)))
    }

    @Test
    fun `충전구가 닫히지 않은 채 주행이 시작되면 그 시점에 끝낸다`() {
        val d = ChargeDetector()
        d.onSample(s(0, port = true, pct = 20, kwh = 15.0))
        d.onSample(s(100_000, pct = 25, kwh = 19.0))
        val c = d.onDriveState(DriveState.DRIVING, 200_000)
        assertNotNull(c)
        assertEquals(200_000L, c!!.endedAtMs)
        assertEquals(5, c.endPercent - c.startPercent)
    }

    @Test
    fun `잔량 kWh가 없으면 용량 곱하기 % 차이로 충전량을 계산한다`() {
        val d = ChargeDetector()
        d.onSample(s(0, port = false, pct = 50), capacityKwh = 80.0)
        d.onSample(s(1_000, port = true), capacityKwh = 80.0)
        d.onSample(s(2_000, pct = 60), capacityKwh = 80.0)
        val c = d.onSample(s(3_000, port = false), capacityKwh = 80.0)!!
        assertEquals(8.0, c.addedKwh, 1e-6)
    }

    @Test
    fun `충전구를 열 때 %를 모르면 처음 들어온 %를 시작값으로 쓴다`() {
        val d = ChargeDetector()
        d.onSample(s(0, port = true))
        d.onSample(s(1_000, pct = 41))
        d.onSample(s(2_000, pct = 45))
        val c = d.onSample(s(3_000, port = false))!!
        assertEquals(41, c.startPercent)
        assertEquals(45, c.endPercent)
    }

    @Test
    fun `충전구 신호가 계속 열림이면 새 세션을 다시 열지 않는다`() {
        val d = ChargeDetector()
        d.onSample(s(0, port = true, pct = 10))
        d.onSample(s(1_000, port = true, pct = 12))
        d.onSample(s(2_000, port = true, pct = 15))
        val c = d.onSample(s(3_000, port = false))!!
        assertEquals(0L, c.startedAtMs)
        assertEquals(10, c.startPercent)
    }
}
