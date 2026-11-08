package com.probe.topbar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** adb shell am broadcast -n com.probe.topbar/.CmdReceiver -a T3 --el arg 60000 */
class CmdReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Probe.init(context)
        Probe.run(intent.action ?: return, intent.getLongExtra("arg", 0))
    }
}
