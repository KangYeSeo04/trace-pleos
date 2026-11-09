package com.tracers.trace.ui.coin

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
import com.tracers.trace.ui.screens.coin.CoinScreen
import com.tracers.trace.ui.theme.TraceTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 디자인 비교용 캡처: 샘플 상태를 600 × 800 칸(배율 1)으로 그려 위·가운데·아래 3구간을 저장한다.
 * 결과: 앱 내부 files/coin_capture/coin_{top,mid,bottom}.png (AAOS 사용자 10이라 run-as --user 10 으로 꺼낸다)
 */
@RunWith(AndroidJUnit4::class)
class CoinScreenCaptureTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun captureSections() {
        val scroll = ScrollState(0)
        // 히어로 애니메이션이 끝없이 프레임을 요청하므로 시계를 직접 돌린다
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(CAPTURE_DENSITY, 1f)) {
                Box(Modifier.size(600.dp, 800.dp).testTag("root")) {
                    TraceTheme { CoinScreen(uiState = SampleData.coinUiState, onNavigateBack = {}, scrollState = scroll) }
                }
            }
        }
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "coin_capture")
        dir.mkdirs()
        listOf("top" to 0f, "mid" to 600f, "bottom" to Float.MAX_VALUE).forEach { (name, designPx) ->
            rule.runOnUiThread {
                scroll.dispatchRawDelta(-scroll.value.toFloat())
                scroll.dispatchRawDelta(if (designPx == Float.MAX_VALUE) 1e7f else designPx * CAPTURE_DENSITY)
            }
            rule.mainClock.advanceTimeBy(1_000)
            val bmp = rule.onNodeWithTag("root").captureToImage().asAndroidBitmap()
            File(dir, "coin_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private companion object {
        /** 디자인 1px = 1.4 화면 px. 테스트 창 너비가 841px(3분할 칸)이라 600 × 1.4 = 840 */
        const val CAPTURE_DENSITY = 1.4f
    }
}
