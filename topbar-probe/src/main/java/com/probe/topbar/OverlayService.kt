package com.probe.topbar

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager

/**
 * T8 이어서: 캐릭터 오버레이 창.
 * action: START / POS_A..POS_F / STOP
 * F는 추가 확인용: E와 같은 플래그로 오른쪽 위(상단바 아이콘 영역 위)에 y=0
 * adb shell am start-foreground-service --user 10 -n com.probe.topbar/.OverlayService -a POS_B
 */
class OverlayService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val wm by lazy { getSystemService(WindowManager::class.java) }
    private var view: CharacterView? = null
    private var preset = "A"

    private val tick = object : Runnable {
        override fun run() {
            view?.let { it.frame = 1 - it.frame; it.invalidate() }
            main.postDelayed(this, 500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Probe.init(this)
        val action = intent?.action ?: "START"
        Log.i(TAG, "overlay service action=$action")
        if (action == "STOP") {
            removeView()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(ID_FGS, Probe.fgsNotification(Probe.CH_LOW),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        if (action.startsWith("POS_")) preset = action.removePrefix("POS_")
        show()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "overlay service onDestroy")
        removeView()
        super.onDestroy()
    }

    private fun params(): WindowManager.LayoutParams {
        val size = (64 * resources.displayMetrics.density).toInt()
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        if (preset == "E" || preset == "F") flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return WindowManager.LayoutParams(size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = when (preset) {
                "B", "F" -> Gravity.TOP or Gravity.END
                "C" -> Gravity.BOTTOM or Gravity.START
                "D" -> Gravity.BOTTOM or Gravity.END
                else -> Gravity.TOP or Gravity.START
            }
            x = 0; y = 0
            title = "ProbeCharacter"
        }
    }

    private fun show() {
        val can = Settings.canDrawOverlays(this)
        Log.i(TAG, "overlay canDrawOverlays=$can preset=$preset")
        val lp = params()
        val v = view
        try {
            if (v == null) {
                val nv = CharacterView(this)
                wm.addView(nv, lp)
                view = nv
                main.removeCallbacks(tick)
                main.post(tick)
                nv.post { logPosition(nv) }
            } else {
                wm.updateViewLayout(v, lp)
                v.postDelayed({ logPosition(v) }, 300)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "overlay addView/update failed preset=$preset: $t", t)
        }
    }

    private fun logPosition(v: View) {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        val bars = v.rootWindowInsets?.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val dimen = if (resId != 0) resources.getDimensionPixelSize(resId) else -1
        Log.i(TAG, "overlay preset=$preset locationOnScreen x=${loc[0]} y=${loc[1]} w=${v.width} h=${v.height} " +
                "statusBarInsetTop=${bars?.top}px status_bar_height(dimen)=${dimen}px")
    }

    private fun removeView() {
        main.removeCallbacks(tick)
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    /** 색깔 원 + 눈 두 개. frame 0/1 번갈아 눈 위치와 몸 높이가 살짝 바뀜 */
    private class CharacterView(ctx: Context) : View(ctx) {
        var frame = 0
        private val body = Paint(Paint.ANTI_ALIAS_FLAG)
        private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val bob = if (frame == 0) 0f else w * 0.06f
            body.color = if (frame == 0) Color.rgb(255, 140, 0) else Color.rgb(255, 170, 40)
            val cy = w / 2 + bob
            canvas.drawCircle(w / 2, cy, w * 0.44f, body)
            val eyeDx = w * 0.16f
            val look = if (frame == 0) -w * 0.04f else w * 0.04f
            for (sx in floatArrayOf(-1f, 1f)) {
                canvas.drawCircle(w / 2 + sx * eyeDx, cy - w * 0.08f, w * 0.1f, white)
                canvas.drawCircle(w / 2 + sx * eyeDx + look, cy - w * 0.08f, w * 0.05f, black)
            }
        }
    }

    companion object {
        const val TAG = "probe"
        const val ID_FGS = 2
    }
}
