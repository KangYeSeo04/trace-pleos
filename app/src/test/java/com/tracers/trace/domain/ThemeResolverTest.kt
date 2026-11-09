package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A-03 테마 결정: 스위치 선택은 시스템 테마가 바뀌기 전까지만 유지 */
class ThemeResolverTest {

    @Test
    fun 선택_없으면_시스템을_따름() {
        assertTrue(resolveDarkTheme(choice = null, systemDark = true))
        assertFalse(resolveDarkTheme(choice = null, systemDark = false))
    }

    @Test
    fun 시스템이_그대로면_선택을_따름() {
        // 시스템 라이트일 때 다크를 골랐다
        val choice = ThemeChoice(dark = true, systemDarkWhenChosen = false)
        assertTrue(resolveDarkTheme(choice, systemDark = false))
    }

    @Test
    fun 시스템이_바뀌면_선택은_무효() {
        val choice = ThemeChoice(dark = true, systemDarkWhenChosen = false)
        // 시스템이 다크로 → 다크 (선택과 같아도 시스템 때문)
        assertTrue(resolveDarkTheme(choice, systemDark = true))
        assertTrue(isChoiceStale(choice, systemDark = true))
        assertFalse(isChoiceStale(choice, systemDark = false))
    }

    @Test
    fun 시스템이_바뀐_뒤_라이트로_돌아와도_다시_시스템() {
        // 시스템 다크일 때 라이트를 골랐는데 시스템이 라이트로 → 라이트 (저장값은 지워야 함)
        val choice = ThemeChoice(dark = false, systemDarkWhenChosen = true)
        assertEquals(false, resolveDarkTheme(choice, systemDark = false))
        assertTrue(isChoiceStale(choice, systemDark = false))
    }
}
