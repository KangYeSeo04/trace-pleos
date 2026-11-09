package com.tracers.trace.data.settings

import android.content.Context
import com.tracers.trace.domain.ThemeChoice
import com.tracers.trace.domain.isChoiceStale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 해·달 스위치 선택 (A-03). 기기 안 SharedPreferences에 저장.
 * 고를 때의 시스템 테마도 같이 저장해, 시스템 테마가 바뀌면 선택을 지우고 다시 시스템을 따른다.
 */
class ThemeSetting(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _choice = MutableStateFlow(load())

    /** null이면 선택 없음(시스템 테마를 따름) */
    val choice: StateFlow<ThemeChoice?> = _choice.asStateFlow()

    fun set(dark: Boolean, systemDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, dark).putBoolean(KEY_SYSTEM_DARK, systemDark).apply()
        _choice.value = ThemeChoice(dark, systemDark)
    }

    /** 시스템 테마가 고를 때와 달라졌으면 선택을 지운다 (앱이 꺼져 있는 동안 바뀐 경우 포함) */
    fun onSystemTheme(systemDark: Boolean) {
        val c = _choice.value ?: return
        if (isChoiceStale(c, systemDark)) {
            prefs.edit().remove(KEY_DARK).remove(KEY_SYSTEM_DARK).apply()
            _choice.value = null
        }
    }

    // 고를 때의 시스템 테마가 없는 예전 저장값은 버린다
    private fun load(): ThemeChoice? =
        if (prefs.contains(KEY_DARK) && prefs.contains(KEY_SYSTEM_DARK)) {
            ThemeChoice(prefs.getBoolean(KEY_DARK, true), prefs.getBoolean(KEY_SYSTEM_DARK, true))
        } else null

    private companion object {
        const val PREFS = "theme_settings"
        const val KEY_DARK = "dark"
        const val KEY_SYSTEM_DARK = "system_dark_when_chosen"
    }
}
