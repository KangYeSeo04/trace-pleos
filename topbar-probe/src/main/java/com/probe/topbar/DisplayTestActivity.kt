package com.probe.topbar

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.widget.TextView

/** T10: 디스플레이 목록을 찍고, 기본 외 디스플레이에 EdgeActivity 실행 시도 */
class DisplayTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Probe.init(this)
        val sb = StringBuilder()
        for (id in Probe.logDisplays()) {
            sb.append("display $id\n")
            if (id == Display.DEFAULT_DISPLAY) continue
            try {
                val opts = ActivityOptions.makeBasic().setLaunchDisplayId(id)
                startActivity(Intent(this, EdgeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK), opts.toBundle())
                Log.i(Probe.TAG, "T10 launch on display $id ok")
                sb.append("  launch ok\n")
            } catch (t: Throwable) {
                Log.e(Probe.TAG, "T10 launch on display $id failed: $t")
                sb.append("  launch failed: $t\n")
            }
        }
        setContentView(TextView(this).apply { textSize = 20f; text = sb; setPadding(32, 32, 32, 32) })
    }
}
