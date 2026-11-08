package com.probe.topbar

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

/**
 * 모든 테스트 로직. Activity 버튼, CmdReceiver(adb broadcast), ProbeService가 모두 여기로 들어온다.
 * 로그 태그는 전부 "probe".
 */
@SuppressLint("StaticFieldLeak")
object Probe {
    const val TAG = "probe"

    const val CH_DEFAULT = "probe_default"
    const val CH_HIGH = "probe_high"
    const val CH_LOW = "probe_low"
    const val CH_MSG = "probe_msg"
    const val CH_FGS_DEFAULT = "probe_fgs_default"

    const val ID_T1 = 101
    const val ID_T2 = 102
    const val ID_NAV = 103
    const val ID_T5 = 105
    const val ID_FGS = 1

    private val main = Handler(Looper.getMainLooper())
    private var navLoop: Runnable? = null
    private var overlay: View? = null
    private lateinit var app: Context

    fun init(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_DEFAULT, "Probe DEFAULT", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_HIGH, "Probe HIGH", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_LOW, "Probe LOW", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CH_MSG, "Probe MESSAGE", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_FGS_DEFAULT, "Probe FGS DEFAULT", NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
        Log.i(TAG, "init: areNotificationsEnabled=${nm.areNotificationsEnabled()}")
    }

    private val nm get() = app.getSystemService(NotificationManager::class.java)

    /** 명령 문자열로 테스트 실행. arg는 주기(ms) 등 */
    fun run(cmd: String, arg: Long = 0) {
        Log.i(TAG, "run cmd=$cmd arg=$arg")
        try {
            when (cmd) {
                "T1" -> t1()
                "T2" -> t2()
                "T3" -> t3(if (arg > 0) arg else 60_000)
                "T3_STOP" -> stopNav(cancel = false)
                "T4" -> t4(if (arg > 0) arg else 1000)
                "T5" -> t5()
                "T7" -> main.postDelayed({ toast("probe 백그라운드 토스트 T7") }, if (arg > 0) arg else 5000)
                "T8A" -> t8(variantB = false)
                "T8B" -> t8(variantB = true)
                "T8_REMOVE" -> removeOverlay()
                "T10" -> logDisplays()
                "T12" -> t12()
                "CLEAR" -> { stopNav(cancel = true); nm.cancelAll(); removeOverlay() }
                else -> Log.w(TAG, "unknown cmd $cmd")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "cmd $cmd failed: $t", t)
            toast("$cmd 실패: $t")
        }
    }

    private fun base(channel: String) = Notification.Builder(app, channel)
        .setSmallIcon(android.R.drawable.ic_dialog_info)

    private fun t1() {
        nm.notify(ID_T1, base(CH_DEFAULT)
            .setContentTitle("T1 일반 알림")
            .setContentText("IMPORTANCE_DEFAULT, 카테고리 없음")
            .build())
    }

    private fun t2() {
        nm.notify(ID_T2, base(CH_HIGH)
            .setContentTitle("T2 HIGH 무카테고리")
            .setContentText("IMPORTANCE_HIGH, 카테고리 없음")
            .build())
    }

    private fun navNotification(seq: Int, frame: Int): Notification =
        base(CH_HIGH)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setContentTitle("교차로 300m")
            .setContentText("2차로 유지 #$seq")
            .setLargeIcon(arrowFrame(frame))
            .setOnlyAlertOnce(false)
            .build()

    /** T3: 같은 ID로 5초마다 갱신, durationMs 동안 */
    private fun t3(durationMs: Long) = startNavLoop(periodMs = 5000, durationMs = durationMs, animate = false)

    /** T4: 큰 아이콘 4프레임 순환. 30초간 */
    private fun t4(periodMs: Long) =
        startNavLoop(periodMs = periodMs.coerceAtLeast(250), durationMs = 30_000, animate = true)

    private fun startNavLoop(periodMs: Long, durationMs: Long, animate: Boolean) {
        stopNav(cancel = false)
        val start = System.currentTimeMillis()
        var seq = 0
        val loop = object : Runnable {
            override fun run() {
                if (System.currentTimeMillis() - start > durationMs) {
                    Log.i(TAG, "nav loop done after $seq updates (갱신 중단)")
                    navLoop = null
                    return
                }
                val frame = if (animate) seq % 4 else 0
                nm.notify(ID_NAV, navNotification(seq, frame))
                Log.i(TAG, "nav notify seq=$seq frame=$frame period=$periodMs")
                seq++
                main.postDelayed(this, periodMs)
            }
        }
        navLoop = loop
        main.post(loop)
    }

    private fun stopNav(cancel: Boolean) {
        navLoop?.let { main.removeCallbacks(it); Log.i(TAG, "nav loop stopped") }
        navLoop = null
        if (cancel) nm.cancel(ID_NAV)
    }

    private fun t5() {
        val me = Person.Builder().setName("나").build()
        val sender = Person.Builder().setName("Probe 친구").build()
        nm.notify(ID_T5, base(CH_MSG)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setStyle(Notification.MessagingStyle(me)
                .addMessage("T5 메시지 헤드업 테스트", System.currentTimeMillis(), sender))
            .build())
    }

    fun fgsNotification(channel: String): Notification = base(channel)
        .setContentTitle("T6 포그라운드 서비스")
        .setContentText("channel=$channel ongoing")
        .setOngoing(true)
        .build()

    fun toast(msg: String) = main.post { Toast.makeText(app, msg, Toast.LENGTH_LONG).show() }

    private fun t8(variantB: Boolean) {
        removeOverlay()
        val can = Settings.canDrawOverlays(app)
        Log.i(TAG, "T8 canDrawOverlays=$can variantB=$variantB")
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (variantB) flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        val heightPx = (60 * app.resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP; y = 0 }
        val v = TextView(app).apply {
            text = "T8 overlay ${if (variantB) "B" else "A"}"
            setTextColor(Color.WHITE)
            textSize = 20f
            setBackgroundColor(Color.argb(160, 255, 0, 0))
        }
        val wm = app.getSystemService(WindowManager::class.java)
        try {
            wm.addView(v, lp)
            overlay = v
            v.post {
                val loc = IntArray(2); v.getLocationOnScreen(loc)
                Log.i(TAG, "T8 overlay added: screenY=${loc[1]} h=${v.height} heightPx=$heightPx")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "T8 addView failed: $t", t)
            toast("T8 실패: $t")
        }
    }

    /** T12: 상단바 아이콘 슬롯 직접 등록 시도 (StatusBarManager.setIcon / IStatusBarService.setIcon). 결과만 기록 */
    private fun t12() {
        val perm = app.checkSelfPermission("android.permission.STATUS_BAR")
        Log.i(TAG, "T12 STATUS_BAR permission granted=${perm == android.content.pm.PackageManager.PERMISSION_GRANTED}")
        val sbm = app.getSystemService("statusbar")
        Log.i(TAG, "T12 statusbar service object=${sbm?.javaClass?.name}")

        try {
            val m = Class.forName("android.app.StatusBarManager").getMethod(
                "setIcon", String::class.java, Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType, String::class.java)
            m.invoke(sbm, "probe_topbar", android.R.drawable.ic_dialog_info, 0, "probe")
            Log.i(TAG, "T12 StatusBarManager.setIcon returned without exception")
        } catch (t: Throwable) {
            val cause = (t as? java.lang.reflect.InvocationTargetException)?.targetException ?: t
            Log.e(TAG, "T12 StatusBarManager.setIcon failed: $cause")
        }

        try {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java).invoke(null, "statusbar")
            val svc = Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
                .getMethod("asInterface", android.os.IBinder::class.java).invoke(null, binder)
            svc!!.javaClass.getMethod("setIcon", String::class.java, String::class.java,
                Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                .invoke(svc, "probe_topbar", app.packageName, android.R.drawable.ic_dialog_info, 0, "probe")
            Log.i(TAG, "T12 IStatusBarService.setIcon returned without exception")
        } catch (t: Throwable) {
            val cause = (t as? java.lang.reflect.InvocationTargetException)?.targetException ?: t
            Log.e(TAG, "T12 IStatusBarService.setIcon failed: $cause")
        }
    }

    private fun removeOverlay() {
        overlay?.let { runCatching { app.getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null
    }

    fun logDisplays(): List<Int> {
        val dm = app.getSystemService(DisplayManager::class.java)
        return dm.displays.map { d ->
            val mode = d.mode
            Log.i(TAG, "T10 display id=${d.displayId} name=${d.name} size=${mode.physicalWidth}x${mode.physicalHeight} " +
                    "flags=0x${Integer.toHexString(d.flags)} state=${d.state} valid=${d.isValid}")
            d.displayId
        }
    }

    /** 원 안의 화살표. frame마다 90도씩 회전 */
    private fun arrowFrame(frame: Int): Bitmap {
        val s = 128
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val colors = intArrayOf(Color.rgb(0, 150, 255), Color.rgb(0, 200, 120), Color.rgb(255, 170, 0), Color.rgb(230, 60, 60))
        c.drawCircle(s / 2f, s / 2f, s / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors[frame % 4] })
        c.rotate(90f * frame, s / 2f, s / 2f)
        val p = Path().apply {
            moveTo(s / 2f, 16f); lineTo(s - 24f, s / 2f); lineTo(s / 2f + 14f, s / 2f)
            lineTo(s / 2f + 14f, s - 18f); lineTo(s / 2f - 14f, s - 18f); lineTo(s / 2f - 14f, s / 2f)
            lineTo(24f, s / 2f); close()
        }
        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        return bmp
    }
}
