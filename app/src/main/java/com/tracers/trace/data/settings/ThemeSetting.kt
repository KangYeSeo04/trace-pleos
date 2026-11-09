package com.tracers.trace.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 해·달 스위치 선택 (A-03). 기기 안 SharedPreferences에 저장.
 * 값이 없으면(null) 시스템 다크/라이트 설정을 따른다. 한 번 누르면 그 선택을 계속 따른다.
 */
class ThemeSetting(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _dark = MutableStateFlow(if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, true) else null)

    /** true 다크, false 라이트, null 시스템 설정 따름 */
    val dark: StateFlow<Boolean?> = _dark.asStateFlow()

    fun set(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, dark).apply()
        _dark.value = dark
    }

    private companion object {
        const val PREFS = "theme_settings"
        const val KEY_DARK = "dark"
    }
}
