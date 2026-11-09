package com.tracers.trace.ui.region

import android.graphics.Bitmap
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.ui.screens.region.RegionMapStore
import com.tracers.trace.ui.screens.region.RegionScreen
import com.tracers.trace.ui.theme.TraceTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 디자인 비교용 캡처: 샘플 상태를 600 × 800 칸(배율 1)으로 그려 위·아래 2구간을 저장한다.
 * 결과: files/region_capture/region_{top,bottom}.png (AAOS 사용자 10이라 run-as --user 10 으로 꺼낸다)
 */
@RunWith(AndroidJUnit4::class)
class RegionScreenCaptureTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun captureSections() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { RegionMapStore.load(context) } // 지도 데이터를 먼저 읽어 둔다
        val scroll = ScrollState(0)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(CAPTURE_DENSITY, 1f)) {
                Box(Modifier.size(600.dp, 800.dp).testTag("root")) {
                    TraceTheme { RegionScreen(uiState = SampleData.regionUiState, onNavigateBack = {}, scrollState = scroll) }
                }
            }
        }
        val dir = File(context.filesDir, "region_capture")
        dir.mkdirs()
        listOf("top" to 0f, "bottom" to 1e7f).forEach { (name, px) ->
            rule.runOnUiThread {
                scroll.dispatchRawDelta(-scroll.value.toFloat())
                scroll.dispatchRawDelta(px)
            }
            rule.waitForIdle()
            Thread.sleep(1_000) // 지도 비트맵은 백그라운드에서 그려진다
            rule.waitForIdle()
            val bmp = rule.onNodeWithTag("root").captureToImage().asAndroidBitmap()
            File(dir, "region_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private companion object {
        /** 디자인 1px = 1.4 화면 px. 테스트 창 너비가 841px(3분할 칸)이라 600 × 1.4 = 840 */
        const val CAPTURE_DENSITY = 1.4f
    }
}
