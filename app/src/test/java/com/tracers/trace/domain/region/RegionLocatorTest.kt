package com.tracers.trace.domain.region

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * 실제 앱 데이터(assets/regions_lookup.json)로 위경도 → 시·군·구 → 161 지역 판정.
 * 1회차 판정 14곳은 tools/geo/check_regions.py의 PROBES와 같다.
 */
class RegionLocatorTest {

    companion object {
        private lateinit var data: RegionLookupData

        @BeforeClass
        @JvmStatic
        fun load() {
            // 단위 테스트 작업 폴더 = app/
            data = RegionLookupParser.parse(File("src/main/assets/regions_lookup.json").readText())
        }
    }

    private fun locate(lat: Double, lon: Double): RegionMatch? = RegionLocator(data).locate(lat, lon)

    private fun assertAt(lat: Double, lon: Double, unit: String, region: String, label: String? = null) {
        val m = locate(lat, lon)
        assertNotNull("($lat, $lon) 판정 없음", m)
        assertEquals("시·군·구", unit, m!!.unitCode)
        assertEquals("161 지역", region, m.regionCode)
        if (label != null) assertEquals(label, m.label)
    }

    @Test fun dataHas161RegionsAnd256Units() {
        assertEquals(161, data.regions.size)
        assertEquals(256, data.units.size)
    }

    // ----- 1회차 판정 14곳 -----

    @Test fun seoulCityHall() = assertAt(37.5665, 126.9780, "11140", "11000", "서울 중구")
    @Test fun yeongjongIsland() = assertAt(37.4602, 126.4407, "28155", "28000", "인천 영종구")
    @Test fun dongincheon() = assertAt(37.4754, 126.6326, "28125", "28000", "인천 제물포구")
    @Test fun geomdan() = assertAt(37.6000, 126.6750, "28290", "28000", "인천 검단구")
    @Test fun gunwiInDaegu() = assertAt(36.2428, 128.5730, "27720", "27000", "대구 군위군")
    @Test fun sejong() = assertAt(36.5040, 127.2620, "36110", "36000", "세종")
    @Test fun gwangjuCityHall() = assertAt(35.1600, 126.8526, "12240", "12200", "광주 서구")
    @Test fun dokdo() = assertAt(37.2410, 131.8650, "47940", "47940", "울릉군")
    @Test fun ulleung() = assertAt(37.4844, 130.9057, "47940", "47940", "울릉군")
    @Test fun marado() = assertAt(33.1180, 126.2670, "50130", "50130", "서귀포시")
    @Test fun chujado() = assertAt(33.9550, 126.3000, "50110", "50110", "제주시")
    @Test fun baengnyeong() = assertAt(37.9600, 124.7150, "28720", "28000", "인천 옹진군")
    @Test fun heuksan() = assertAt(34.6800, 125.4300, "12870", "12870", "신안군")
    /** 완주군에 둘러싸인 전주시: 완주군으로 잘못 붙지 않아야 한다 */
    @Test fun jeonjuInsideWanju() = assertAt(35.8242, 127.1480, "52111", "52110", "전주시 완산구")

    // ----- 확인 시나리오 경로 (판교 → 강남 → 중랑) -----

    @Test fun pangyo() = assertAt(37.3948, 127.1110, "41135", "41130", "성남시 분당구")
    @Test fun gangnamGuOffice() = assertAt(37.5172, 127.0473, "11680", "11000", "서울 강남구")
    @Test fun jungnang() = assertAt(37.6063, 127.0928, "11260", "11000", "서울 중랑구")
    @Test fun wanju() = assertAt(35.9045, 127.1621, "52710", "52710", "완주군")
    @Test fun suwonGeneralGuMergesToCity() = assertAt(37.3039, 127.0102, "41111", "41110", "수원시 장안구")

    // ----- 경계 밖 -----

    @Test fun offshoreGpsSnapsToNearestUnit() {
        val m = locate(35.150, 129.165)  // 해운대 앞바다 약 1 km
        assertEquals("26350", m?.unitCode)
        assertTrue(m!!.snapped)
    }

    @Test fun bridgeOverSeaSnapsToNearestUnit() {
        assertEquals("28185", locate(37.41, 126.56)?.unitCode)  // 인천대교 중간
    }

    @Test fun farAtSeaOrAbroadIsNull() {
        assertNull(locate(33.5, 128.0))      // 남해 먼바다 (약 80 km)
        assertNull(locate(35.68, 139.69))    // 도쿄
    }

    @Test fun insideHitIsNotSnapped() {
        assertFalse(locate(37.6063, 127.0928)!!.snapped)
    }

    // ----- 직전 지역 먼저 -----

    @Test fun sameLocatorAcrossBorderFollowsMovement() {
        val loc = RegionLocator(data)
        assertEquals("41135", loc.locate(37.3948, 127.1110)?.unitCode)  // 판교
        assertEquals("41135", loc.locate(37.3950, 127.1112)?.unitCode)  // 같은 곳 (직전 지역에서 바로 찾음)
        assertEquals("11680", loc.locate(37.5172, 127.0473)?.unitCode)  // 강남
        assertEquals("11260", loc.locate(37.6063, 127.0928)?.unitCode)  // 중랑
    }

    @Test fun regionInfoForCode() {
        val r = data.regionByCode.getValue("12200")
        assertEquals("광주", r.name)
        assertEquals("jn", r.group)
        assertEquals(data.regions.size, data.regionByCode.size)
    }
}
