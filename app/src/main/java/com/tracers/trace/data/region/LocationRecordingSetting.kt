package com.tracers.trace.data.region

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 위치 기록 켜기·끄기 (G-04). 기기 안 SharedPreferences에 저장, 기본 켜짐.
 * 끄면 위치 요청과 새 기록만 멈추고 쌓인 기록은 그대로 둔다.
 */
class LocationRecordingSetting(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _on = MutableStateFlow(prefs.getBoolean(KEY_ON, true))
    val on: StateFlow<Boolean> = _on.asStateFlow()

    fun set(on: Boolean) {
        prefs.edit().putBoolean(KEY_ON, on).apply()
        _on.value = on
    }

    private companion object {
        const val PREFS = "region_settings"
        const val KEY_ON = "location_recording_on"
    }
}
