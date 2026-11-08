package com.probe.topbar

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 버튼 하나 = 테스트 하나 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Probe.init(this)
        requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        col.addView(TextView(this).apply { text = "Topbar Probe"; textSize = 24f })

        fun btn(label: String, onClick: () -> Unit) = col.addView(Button(this).apply {
            text = label; setOnClickListener { onClick() }
        })
        fun svc(action: String, arg: Long = 0) =
            startForegroundService(Intent(this, ProbeService::class.java).setAction(action).putExtra("arg", arg))

        btn("T1 일반 알림") { Probe.run("T1") }
        btn("T2 HIGH 무카테고리") { Probe.run("T2") }
        btn("T3 NAVIGATION 헤드업 60초 (서비스)") { svc("T3", 60_000) }
        btn("T3 갱신 중단") { Probe.run("T3_STOP") }
        btn("T4 아이콘 애니 1초") { svc("T4", 1000) }
        btn("T4 아이콘 애니 0.5초") { svc("T4", 500) }
        btn("T4 아이콘 애니 0.25초") { svc("T4", 250) }
        btn("T5 MESSAGE 헤드업") { Probe.run("T5") }
        btn("T6 FGS LOW") { svc("FGS_LOW") }
        btn("T6 FGS DEFAULT") { svc("FGS_DEFAULT") }
        btn("T6 FGS 중지") { startService(Intent(this, ProbeService::class.java).setAction("STOP")) }
        btn("T7 5초 후 토스트 (서비스)") { svc("T7", 5000) }
        btn("T8 오버레이 권한 설정 화면 열기") {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            val resolved = i.resolveActivity(packageManager)
            Log.i(Probe.TAG, "T8 overlay settings intent resolves to $resolved")
            if (resolved != null) startActivity(i)
        }
        btn("T8 오버레이 A (서비스)") { svc("T8A") }
        btn("T8 오버레이 B (서비스)") { svc("T8B") }
        btn("T8 오버레이 제거") { Probe.run("T8_REMOVE") }
        fun overlay(action: String) =
            startForegroundService(Intent(this, OverlayService::class.java).setAction(action))
        btn("T8 캐릭터 오버레이 시작") { overlay("START") }
        for (p in listOf("A", "B", "C", "D", "E", "F")) btn("T8 캐릭터 위치 $p") { overlay("POS_$p") }
        btn("T8 캐릭터 오버레이 중지") { overlay("STOP") }
        btn("T9 edge-to-edge") { startActivity(Intent(this, EdgeActivity::class.java)) }
        btn("T10 디스플레이") { startActivity(Intent(this, DisplayTestActivity::class.java)) }
        btn("T12 상단바 아이콘 등록 시도") { Probe.run("T12") }
        btn("모두 지우기") { Probe.run("CLEAR") }

        setContentView(ScrollView(this).apply { addView(col) })
    }
}
