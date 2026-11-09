package com.tracers.trace.ui.coin

import com.tracers.trace.ui.screens.coin.HeroMotion
import org.junit.Assert.assertEquals
import org.junit.Test

/** CoinDark.dc.html 캔버스 보조 함수가 JS와 같은 값을 내는지 (기대값은 node로 원본 JS를 돌려 얻음). */
class HeroMotionTest {

    @Test
    fun hashStr_JS와_같다() {
        assertEquals(3885284911L, HeroMotion.hashStr("coincruise"))
    }

    @Test
    fun 시작시각_t0는_191초() {
        assertEquals(191.0, HeroMotion.T0, 0.0)
    }

    @Test
    fun vn1_JS와_같다() {
        assertEquals(0.509459348709729, HeroMotion.vn1(123.45, 14), 1e-12)
        assertEquals(0.3841277537791447, HeroMotion.vn1(-7.3, 72), 1e-12)
    }
}
