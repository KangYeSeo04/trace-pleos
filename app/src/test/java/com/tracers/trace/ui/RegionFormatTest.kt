package com.tracers.trace.ui

import com.tracers.trace.ui.screens.region.RegionFormat
import com.tracers.trace.ui.screens.region.RegionUiState
import com.tracers.trace.ui.screens.region.VisitedRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionFormatTest {
    @Test
    fun distance() {
        assertEquals("128 km", RegionFormat.distance(128.4))
        assertEquals("9,840 km", RegionFormat.distance(9840.0))
        assertEquals("3.2 km", RegionFormat.distance(3.24))
        assertEquals("0 km", RegionFormat.distance(0.0))
    }

    @Test
    fun stayMatchesDesignExamples() {
        assertEquals("0분", RegionFormat.stay(0))
        assertEquals("45분", RegionFormat.stay(45))
        assertEquals("6시간", RegionFormat.stay(360))
        assertEquals("3시간 10분", RegionFormat.stay(190))
        assertEquals("1일 2시간", RegionFormat.stay(26 * 60))
        assertEquals("2일 6시간", RegionFormat.stay(3240))
        assertEquals("1개월 5일", RegionFormat.stay(35L * 24 * 60))
        assertEquals("1년 3개월", RegionFormat.stay((365L + 95) * 24 * 60))
    }

    @Test
    fun selectedFallsBackToCurrentRegion() {
        val a = VisitedRegion("47130", "경주시", "경상북도", 8, 128.0, 3240)
        val b = VisitedRegion("11000", "서울", "서울특별시", 0, 9840.0, 600000)
        val s = RegionUiState(visited = listOf(a, b), currentRegionCode = "47130")
        assertEquals("47130", s.selected?.code)
        assertEquals("11000", s.copy(selectedRegionCode = "11000").selected?.code)
        assertNull(RegionUiState().selected)
    }
}
