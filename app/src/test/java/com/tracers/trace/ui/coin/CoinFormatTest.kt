package com.tracers.trace.ui.coin

import com.tracers.trace.ui.screens.coin.formatCoins
import com.tracers.trace.ui.screens.coin.formatSignedCoins
import org.junit.Assert.assertEquals
import org.junit.Test

class CoinFormatTest {
    @Test
    fun 천단위_쉼표() = assertEquals("1,284", formatCoins(1284))

    @Test
    fun 적립은_더하기_구매는_빼기기호() {
        assertEquals("+31", formatSignedCoins(31))
        assertEquals("−500", formatSignedCoins(-500))
    }
}
