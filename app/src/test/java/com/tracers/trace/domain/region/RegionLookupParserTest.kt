package com.tracers.trace.domain.region

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionLookupParserTest {
    private val sample = """
        {"meta": {"tolerance_m": 15.0, "note": "a\"b\\cA"},
         "regions": [{"code": "11000", "name": "서울", "sido": "서울특별시", "group": "seoul",
                      "units": ["11110"], "center": [127.0, 37.5]}],
         "units": [{"code": "11110", "name": "종로구", "fullName": "종로구", "sido": "서울특별시",
                    "sidoShort": "서울", "region": "11000", "center": [126.9, 37.6],
                    "bbox": [126.0, 37.0, 127.5, 38.0],
                    "rings": [[126.0, 37.0, 127.5, 37.0, 127.5, 38.0, -1e-3, 3.5E1]]}]}
    """.trimIndent()

    @Test fun parsesRegionsAndUnits() {
        val d = RegionLookupParser.parse(sample)
        assertEquals(1, d.regions.size)
        assertEquals(RegionInfo("11000", "서울", "서울특별시", "seoul"), d.regions[0])
        val u = d.units.single()
        assertEquals("11110", u.code)
        assertEquals("종로구", u.name)
        assertEquals("서울", u.sidoShort)
        assertEquals("11000", u.regionCode)
        assertArrayEquals(doubleArrayOf(126.0, 37.0, 127.5, 38.0), u.bbox, 0.0)
        assertArrayEquals(doubleArrayOf(126.0, 37.0, 127.5, 37.0, 127.5, 38.0, -0.001, 35.0), u.rings.single(), 1e-12)
    }
}
