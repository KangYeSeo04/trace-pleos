package com.tracers.trace.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TraceLayoutTest {

    @Test
    fun `3분할 실측 칸은 Compact`() {
        assertEquals(TraceLayout.Compact, TraceLayout.select(841f, 1210f))
    }

    @Test
    fun `디자인 기준 프레임은 각자 자기 배치`() {
        assertEquals(TraceLayout.Compact, TraceLayout.select(600f, 800f))
        assertEquals(TraceLayout.Wide, TraceLayout.select(1200f, 800f))
    }

    @Test
    fun `가로로 넓은 칸은 Wide`() {
        assertEquals(TraceLayout.Wide, TraceLayout.select(2560f, 1210f))
    }

    @Test
    fun `정사각형에 가까운 칸은 Compact`() {
        assertEquals(TraceLayout.Compact, TraceLayout.select(1000f, 1000f))
    }

    @Test
    fun `세로가 0이면 Compact`() {
        assertEquals(TraceLayout.Compact, TraceLayout.select(1200f, 0f))
    }
}
