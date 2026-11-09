package com.tracers.trace.ui.screens.maintenance

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsTransformationTest {

    private val t = ThousandsTransformation(" km")

    @Test
    fun `천 단위 쉼표와 단위`() {
        assertEquals("38,800 km", t.filter(AnnotatedString("38800")).text.text)
        assertEquals("1,234,567 km", t.filter(AnnotatedString("1234567")).text.text)
        assertEquals("800 km", t.filter(AnnotatedString("800")).text.text)
        assertEquals("", t.filter(AnnotatedString("")).text.text)
    }

    @Test
    fun `커서 위치는 쉼표를 건너 맞고 끝은 단위 앞`() {
        val m = t.filter(AnnotatedString("38800")).offsetMapping
        assertEquals(0, m.originalToTransformed(0))
        assertEquals(2, m.originalToTransformed(2))
        assertEquals(4, m.originalToTransformed(3))
        assertEquals(6, m.originalToTransformed(5))
        assertEquals(5, m.transformedToOriginal(9))
        assertEquals(2, m.transformedToOriginal(3))
        for (o in 0..5) assertEquals(o, m.transformedToOriginal(m.originalToTransformed(o)))
    }
}
