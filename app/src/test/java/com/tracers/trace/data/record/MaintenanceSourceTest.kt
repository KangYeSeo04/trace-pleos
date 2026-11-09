package com.tracers.trace.data.record

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MaintenanceSourceTest {
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun ms(h: Int, m: Int, s: Int = 0) =
        ZonedDateTime.of(2026, 11, 9, h, m, s, 0, seoul).toInstant().toEpochMilli()

    @Test
    fun `다음 자정까지 남은 시간`() {
        assertEquals(60_000L, MaintenanceSource.msUntilNextMidnight(ms(23, 59), seoul))
        assertEquals(86_400_000L, MaintenanceSource.msUntilNextMidnight(ms(0, 0), seoul))
        assertEquals(6 * 3_600_000L, MaintenanceSource.msUntilNextMidnight(ms(18, 0), seoul))
    }
}
