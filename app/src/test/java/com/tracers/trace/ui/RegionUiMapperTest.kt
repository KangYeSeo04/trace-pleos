package com.tracers.trace.ui

import com.tracers.trace.domain.region.RegionInfo
import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.RegionMatch
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisit
import com.tracers.trace.ui.screens.region.RegionUiMapper
import com.tracers.trace.ui.screens.region.VisitedRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionUiMapperTest {
    private val data = RegionLookupData(
        regions = listOf(RegionInfo("11000", "서울", "서울특별시", "seoul"), RegionInfo("41130", "성남시", "경기도", "gg")),
        units = emptyList(),
    )

    @Test fun mapsNamesAndMinutesAndDropsUnknownCodes() {
        val out = RegionUiMapper.visited(
            listOf(
                RegionVisit("41130", 0, 10, 12.5, 125_000),
                RegionVisit("99999", 0, 5, 1.0, 0),
                RegionVisit("11000", 0, 1, 3.0, 59_999),
            ),
            data,
        )
        assertEquals(
            listOf(
                VisitedRegion("41130", "성남시", "경기도", 12.5, 2),
                VisitedRegion("11000", "서울", "서울특별시", 3.0, 0),
            ),
            out,
        )
    }

    @Test fun noDataYetMeansEmpty() {
        assertEquals(emptyList<VisitedRegion>(), RegionUiMapper.visited(listOf(RegionVisit("11000", 0, 0, 1.0, 0)), null))
    }

    @Test fun currentCodeOnlyWhenLocated() {
        val m = RegionMatch("11260", "중랑구", "11000", "서울", "서울특별시", "seoul", showUnit = true, snapped = false)
        assertEquals("11000", RegionUiMapper.currentCode(RegionStatus.Located(m)))
        assertNull(RegionUiMapper.currentCode(RegionStatus.Off))
        assertNull(RegionUiMapper.currentCode(RegionStatus.Unavailable))
    }
}
