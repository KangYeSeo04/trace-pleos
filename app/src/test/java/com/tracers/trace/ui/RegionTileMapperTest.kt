package com.tracers.trace.ui

import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.RegionLookupParser
import com.tracers.trace.domain.region.RegionLocator
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisit
import com.tracers.trace.ui.screens.home.RegionTileMapper
import com.tracers.trace.ui.screens.home.StatusLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class RegionTileMapperTest {
    companion object {
        private lateinit var data: RegionLookupData

        @BeforeClass @JvmStatic
        fun load() {
            data = RegionLookupParser.parse(File("src/main/assets/regions_lookup.json").readText())
        }
    }

    private val visits = listOf(
        RegionVisit("11000", 0, 0, 9_000.0, 0),
        RegionVisit("41130", 0, 0, 840.0, 0),
    )

    private fun at(lat: Double, lon: Double) = RegionStatus.Located(RegionLocator(data).locate(lat, lon)!!)

    @Test fun parkedInSeoulShowsDistrictCountBarDistanceAndMap() {
        val t = RegionTileMapper.map(at(37.6063, 127.0928), visits, data, isDriving = false)
        assertEquals("서울", t.city)
        assertEquals("중랑구", t.district)
        assertEquals("161곳 중 2곳 방문", t.status)
        assertEquals(200f / 161, t.barPercent, 1e-4f)
        assertEquals("달린 거리 9,840 km", t.subText)
        assertTrue(t.showMap)
        // 중랑구는 서울 동북쪽
        assertTrue(t.locationX > 60f && t.locationY < 40f)
    }

    @Test fun drivingShowsDistrictAsStatus() {
        assertEquals("중랑구", RegionTileMapper.map(at(37.6063, 127.0928), visits, data, isDriving = true).status)
    }

    @Test fun outsideSeoulHasNoSeoulMap() {
        val t = RegionTileMapper.map(at(37.3948, 127.1110), visits, data, isDriving = false)
        assertEquals("성남시", t.city)
        assertEquals("분당구", t.district)
        assertFalse(t.showMap)
    }

    @Test fun singleUnitRegionHasNoDistrict() {
        val t = RegionTileMapper.map(at(37.4844, 130.9057), visits, data, isDriving = true)
        assertEquals("울릉군", t.city)
        assertEquals("", t.district)
        assertEquals("161곳 중 2곳 방문", t.status)
    }

    @Test fun offAndUnavailableSayWhy() {
        val off = RegionTileMapper.map(RegionStatus.Off, visits, data, isDriving = false)
        assertEquals("위치 기록이 꺼져 있어요", off.status)
        assertEquals("161곳 중 2곳 방문 · 9,840 km", off.subText)
        assertEquals("–", off.city)
        val na = RegionTileMapper.map(RegionStatus.Unavailable, visits, data, isDriving = false)
        assertEquals("위치를 받지 못하고 있어요", na.status)
        assertEquals(StatusLevel.Caution, na.statusLevel)
        assertEquals("위치를 찾는 중이에요", RegionTileMapper.map(RegionStatus.Searching, emptyList(), data, false).status)
        assertEquals("지도 밖에 있어요", RegionTileMapper.map(RegionStatus.Outside, emptyList(), data, false).status)
    }

    @Test fun beforeDataLoadedNoCount() {
        val t = RegionTileMapper.map(RegionStatus.Searching, visits, null, isDriving = false)
        assertEquals(0f, t.barPercent, 0f)
        assertEquals("9,840 km", t.subText)
    }
}
