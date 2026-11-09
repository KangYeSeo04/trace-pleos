package com.tracers.trace.ui.region

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.ui.screens.region.MapZoomState
import com.tracers.trace.ui.screens.region.RegionMap
import com.tracers.trace.ui.screens.region.RegionMapStore
import com.tracers.trace.ui.screens.region.RegionScreen
import com.tracers.trace.ui.theme.TraceTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

/** 지도 조작: 두 손가락 확대 → 드래그 이동 → 확대 상태에서 탭 선택 → 원래 크기·확대 버튼 */
@RunWith(AndroidJUnit4::class)
class RegionMapInteractionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var map: RegionMap
    private val zoom = MapZoomState()
    private var state by mutableStateOf(SampleData.regionUiState)

    @Before
    fun setUp() {
        map = runBlocking { RegionMapStore.load(InstrumentationRegistry.getInstrumentation().targetContext) }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(D, 1f)) {
                Box(Modifier.size(600.dp, 800.dp)) {
                    TraceTheme {
                        RegionScreen(
                            uiState = state,
                            onNavigateBack = {},
                            onSelectRegion = { state = state.copy(selectedRegionCode = it) },
                            zoom = zoom,
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    /** 지역 중심의 지도 노드 안 px 좌표 (지도 칸 560 × 608, 확대 반영) */
    private fun screenOf(code: String): Offset {
        val r = map.byCode.getValue(code)
        val w = zoom.width; val h = zoom.height
        val k0 = min(w / map.width, h / map.height)
        val x = k0 * r.centerX + (w - map.width * k0) / 2
        val y = k0 * r.centerY + (h - map.height * k0) / 2
        return Offset(D * (x * zoom.scale + zoom.offsetX), D * (y * zoom.scale + zoom.offsetY))
    }

    @Test
    fun pinchDragTapAndButtons() {
        val mapNode = rule.onNodeWithContentDescription("시군 단위 방문 지도", substring = true)

        // 기본 선택 = 지금 지역(경주시)
        rule.onNodeWithText("경상북도").assertIsDisplayed()

        // 탭 선택(1배): 서울
        mapNode.performTouchInput { click(screenOf("11000")) }
        rule.waitForIdle()
        assertEquals("11000", state.selectedRegionCode)

        // 두 손가락 확대
        mapNode.performTouchInput {
            pinch(center - Offset(40f, 0f), center - Offset(200f, 0f), center + Offset(40f, 0f), center + Offset(200f, 0f))
        }
        rule.waitForIdle()
        assertTrue("확대 배율 ${zoom.scale}", zoom.scale > 2f && zoom.scale <= 5f)

        // 드래그 이동
        val before = Offset(zoom.offsetX, zoom.offsetY)
        mapNode.performTouchInput { swipe(center, center + Offset(120f, 80f), durationMillis = 300) }
        rule.waitForIdle()
        assertTrue("이동 $before → ${zoom.offsetX},${zoom.offsetY}", before != Offset(zoom.offsetX, zoom.offsetY))

        // 확대 상태에서 탭 선택: 화면 좌표 → 지도 좌표 변환 확인. 화면 밖이면 원래 크기로 돌린 뒤
        zoom.reset(); zoom.zoomAt(zoom.width / 2, zoom.height / 2, 2f)
        rule.waitForIdle()
        val pk = screenOf("47130")
        mapNode.performTouchInput { click(pk) }
        rule.waitForIdle()
        assertEquals("47130", state.selectedRegionCode)

        // 버튼: 원래 크기, 확대, 축소 (최대 5배)
        rule.onNodeWithContentDescription("지도 원래 크기로").performClick()
        rule.waitForIdle()
        assertEquals(1f, zoom.scale)
        repeat(6) { rule.onNodeWithContentDescription("지도 확대").performClick() }
        rule.waitForIdle()
        assertEquals(5f, zoom.scale)
        rule.onNodeWithContentDescription("지도 축소").performClick()
        rule.waitForIdle()
        assertEquals(5f / 1.5f, zoom.scale, 0.01f)

        // 섬 박스: 제주 박스 빈 바다를 눌러도 제주시 선택
        zoom.reset(); rule.waitForIdle()
        val jeju = map.insets.first { it.id == "jeju" }.frame
        val w = zoom.width; val h = zoom.height
        val k0 = min(w / map.width, h / map.height)
        val fx = k0 * (jeju.left + 6f) + (w - map.width * k0) / 2
        val fy = k0 * (jeju.top + 6f) + (h - map.height * k0) / 2
        mapNode.performTouchInput { click(Offset(fx * D, fy * D)) }
        rule.waitForIdle()
        assertEquals("50110", state.selectedRegionCode)
    }

    private companion object {
        const val D = 1.4f
    }
}
