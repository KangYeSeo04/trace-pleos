package com.tracers.trace.ui

import com.tracers.trace.ui.screens.region.RegionMapStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionMapDataTest {
    @Test
    fun parsesRingsFromBuildScriptFormat() {
        val rings = RegionMapStore.parsePathData("M10 20L30.5 20 30.5 -4 10 -4Z M1 1L2 1 2 2Z")
        assertEquals(2, rings.size)
        assertArrayEquals(floatArrayOf(10f, 20f, 30.5f, 20f, 30.5f, -4f, 10f, -4f), rings[0], 0f)
        assertArrayEquals(floatArrayOf(1f, 1f, 2f, 1f, 2f, 2f), rings[1], 0f)
    }

    @Test
    fun dropsDegenerateRings() {
        assertEquals(0, RegionMapStore.parsePathData("M1 1L2 2Z").size)
    }
}
