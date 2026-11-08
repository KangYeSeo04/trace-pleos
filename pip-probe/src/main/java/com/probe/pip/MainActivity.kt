package com.probe.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** PiP 지원 여부 확인: 기능 플래그, enterPictureInPictureMode 반환값·예외, 모드 변경 콜백 */
class MainActivity : Activity() {

    private lateinit var info: TextView
    private lateinit var button: Button
    private lateinit var cat: RunningCatView
    private val lines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        info = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(32, 32, 32, 16)
        }
        button = Button(this).apply {
            text = "PiP 진입"
            textSize = 20f
            setOnClickListener { enterPip() }
        }
        cat = RunningCatView(this)

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(30, 30, 40))
            addView(info, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(button, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.CENTER_HORIZONTAL })
            addView(cat, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })

        log("hasSystemFeature(FEATURE_PICTURE_IN_PICTURE) = " +
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
        log("SDK_INT=${Build.VERSION.SDK_INT}, MANUFACTURER=${Build.MANUFACTURER}, MODEL=${Build.MODEL}")
        logModes("onCreate")
    }

    private fun enterPip() {
        try {
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(1, 1)).build()
            val ok = enterPictureInPictureMode(params)
            log("enterPictureInPictureMode() = $ok")
        } catch (e: Exception) {
            log("enterPictureInPictureMode() 예외: ${e.javaClass.name}: ${e.message}")
        }
        logModes("enterPip 직후")
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // PiP 창에서는 애니메이션만 보이게
        val v = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        info.visibility = v
        button.visibility = v
        logModes("onPictureInPictureModeChanged($isInPictureInPictureMode) " +
                "${newConfig.screenWidthDp}x${newConfig.screenHeightDp}dp")
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        logModes("onMultiWindowModeChanged($isInMultiWindowMode)")
    }

    // 다른 앱으로 이동했을 때 PiP가 유지되는지(onStop만 오는지, onDestroy까지 오는지) 보기 위해
    override fun onPause() { super.onPause(); logModes("onPause") }
    override fun onStop() { super.onStop(); logModes("onStop") }
    override fun onResume() { super.onResume(); logModes("onResume") }
    override fun onDestroy() { log("onDestroy"); super.onDestroy() }

    private fun logModes(where: String) =
        log("[$where] isInMultiWindowMode=$isInMultiWindowMode, isInPictureInPictureMode=$isInPictureInPictureMode")

    private fun log(text: String) {
        Log.i(TAG, text)
        lines += text
        // 화면에는 처음 2줄(기능 플래그·기기)과 최근 8줄만
        val shown = if (lines.size <= 10) lines else lines.take(2) + "…" + lines.takeLast(8)
        info.text = shown.joinToString("\n")
    }

    companion object {
        const val TAG = "PipTest"
    }
}
