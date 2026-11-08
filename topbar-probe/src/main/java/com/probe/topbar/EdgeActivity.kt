package com.probe.topbar

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** T9: 상단바 뒤까지 그려지는지. 배경 전체 빨간색 */
class EdgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        val label = TextView(this).apply { textSize = 22f; setTextColor(Color.WHITE) }
        val root = FrameLayout(this).apply { setBackgroundColor(Color.RED); addView(label) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cut = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val msg = "T9 statusBars=$sb systemBars=$sys cutout=$cut"
            Log.i(Probe.TAG, msg)
            label.text = msg
            label.setPadding(16, sys.top + 16, 16, 0)
            insets
        }
        setContentView(root)
        root.post {
            val loc = IntArray(2); root.getLocationOnScreen(loc)
            Log.i(Probe.TAG, "T9 root screenY=${loc[1]} h=${root.height} window=${window.decorView.height}")
        }
    }
}
