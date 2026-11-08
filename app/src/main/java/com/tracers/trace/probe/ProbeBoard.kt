package com.tracers.trace.probe

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import java.lang.reflect.InvocationTargetException

/**
 * PROBE 결과판. 항목별 [지원 여부 / 현재 값 / 마지막 갱신 시각 / 콜백 간격 / 오류]를 들고 있고
 * 같은 내용을 Logcat 태그 "PROBE"로 "수신시각(ms) · 항목 · 값" 형식으로 찍는다.
 */
class ProbeBoard {

    data class Row(
        val support: String = "-",
        val value: String = "-",
        val updatedAt: Long = 0,
        val gapMs: Long? = null,
        val error: String = "",
    )

    val order = mutableStateListOf<String>()
    val rows = mutableStateMapOf<String, Row>()
    private val lastAt = HashMap<String, Long>()
    private val main = Handler(Looper.getMainLooper())

    fun support(item: String, text: String) {
        val now = System.currentTimeMillis()
        Log.i(TAG, "$now · $item · 지원=$text")
        update(item) { it.copy(support = text) }
    }

    fun value(item: String, v: Any?) {
        val now = System.currentTimeMillis()
        val gap = synchronized(lastAt) {
            val g = lastAt[item]?.let { now - it }
            lastAt[item] = now
            g
        }
        val text = fmt(v)
        Log.i(TAG, "$now · $item · $text" + (gap?.let { " (+${it}ms)" } ?: ""))
        update(item) { it.copy(value = text, updatedAt = now, gapMs = gap ?: it.gapMs) }
    }

    fun error(item: String, e: Any?) {
        val now = System.currentTimeMillis()
        val text = errText(e)
        Log.w(TAG, "$now · $item · 오류=$text")
        update(item) { it.copy(error = text) }
    }

    /** 표에 줄만 미리 만들어 둔다 (순서 고정용) */
    fun declare(item: String) = update(item) { it }

    private fun update(item: String, f: (Row) -> Row) = main.post {
        if (item !in rows) order += item
        rows[item] = f(rows[item] ?: Row())
    }

    companion object {
        const val TAG = "PROBE"

        fun errText(e: Any?): String {
            val t = (e as? InvocationTargetException)?.targetException ?: e
            return when (t) {
                is SecurityException -> "권한 오류: ${t.message}"
                is Throwable -> "${t.javaClass.simpleName}: ${t.message}"
                else -> "$t"
            }
        }

        fun fmt(v: Any?): String = when (v) {
            null -> "null"
            is Array<*> -> v.joinToString(prefix = "[", postfix = "]") { fmt(it) }
            is IntArray -> v.contentToString()
            is FloatArray -> v.contentToString()
            is LongArray -> v.contentToString()
            is ByteArray -> v.contentToString()
            is Collection<*> -> v.joinToString(prefix = "[", postfix = "]") { fmt(it) }
            is Map<*, *> -> v.entries.joinToString(prefix = "{", postfix = "}") { "${fmt(it.key)}=${fmt(it.value)}" }
            else -> v.toString()
        }
    }
}
