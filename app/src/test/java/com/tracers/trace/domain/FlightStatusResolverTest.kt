package com.tracers.trace.domain

import com.tracers.trace.domain.FlightStatus.ROUGH
import com.tracers.trace.domain.FlightStatus.SLOW
import com.tracers.trace.domain.FlightStatus.SMOOTH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlightStatusResolverTest {

    @Test
    fun `주행이 아니면 주행 문구가 없다`() {
        assertNull(FlightStatusResolver().resolve(DriveState.PARKED, 0.0, rough = true))
        assertNull(FlightStatusResolver().resolve(DriveState.STOPPED, 0.0, rough = false))
    }

    @Test
    fun `거칠었음이 속도보다 먼저다`() {
        assertEquals(ROUGH, FlightStatusResolver().resolve(DriveState.DRIVING, 80.0, rough = true))
    }

    @Test
    fun `처음에는 30kmh 기준으로 천천히·매끄럽게를 나눈다`() {
        assertEquals(SLOW, FlightStatusResolver().resolve(DriveState.DRIVING, 29.0, rough = false))
        assertEquals(SMOOTH, FlightStatusResolver().resolve(DriveState.DRIVING, 30.0, rough = false))
    }

    @Test
    fun `기준 근처에서 왔다 갔다 해도 문구가 깜빡이지 않는다 (25 아래, 35 이상에서만 바뀜)`() {
        val r = FlightStatusResolver()
        val speeds = listOf(40.0, 28.0, 32.0, 26.0, 24.0, 30.0, 34.0, 36.0)
        val out = speeds.map { r.resolve(DriveState.DRIVING, it, rough = false) }
        assertEquals(listOf(SMOOTH, SMOOTH, SMOOTH, SMOOTH, SLOW, SLOW, SLOW, SMOOTH), out)
    }

    @Test
    fun `거칠었음이 풀리면 속도 문구로 돌아온다`() {
        val r = FlightStatusResolver()
        r.resolve(DriveState.DRIVING, 50.0, rough = true)
        assertEquals(SMOOTH, r.resolve(DriveState.DRIVING, 50.0, rough = false))
    }
}
