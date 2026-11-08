package com.probe.topbar

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log

/**
 * 백그라운드 테스트용 포그라운드 서비스.
 * action: FGS_LOW / FGS_DEFAULT / STOP, 그 외 action은 Probe.run()으로 전달(extra "arg").
 */
class ProbeService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Probe.init(this)
        val action = intent?.action ?: "FGS_LOW"
        Log.i(Probe.TAG, "service action=$action")
        if (action == "STOP") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val channel = if (action == "FGS_DEFAULT") Probe.CH_FGS_DEFAULT else Probe.CH_LOW
        try {
            startForeground(Probe.ID_FGS, Probe.fgsNotification(channel),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            Log.i(Probe.TAG, "startForeground ok channel=$channel")
        } catch (t: Throwable) {
            Log.e(Probe.TAG, "startForeground failed: $t", t)
        }
        if (!action.startsWith("FGS_")) Probe.run(action, intent?.getLongExtra("arg", 0) ?: 0)
        return START_NOT_STICKY
    }
}
