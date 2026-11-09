package com.tracers.trace.ui.screens.region

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** 지역 카드 표시 형식 (RegionDark 예: "128 km", "9,840 km", "3시간 10분", "2일 6시간", "1년 3개월"). */
internal object RegionFormat {
    private val symbols = DecimalFormatSymbols(Locale.KOREA)

    /** 10 km 미만은 소수 한 자리, 그 이상은 정수(천 단위 쉼표). */
    fun distance(km: Double): String {
        val v = km.coerceAtLeast(0.0)
        val f = if (v < 10.0) DecimalFormat("0.#", symbols) else DecimalFormat("#,##0", symbols)
        return f.format(v) + " km"
    }

    /** 분 → 가장 큰 두 단위까지. 한 달 = 30일, 한 해 = 365일. */
    fun stay(minutes: Long): String {
        val m = minutes.coerceAtLeast(0)
        val hours = m / 60
        val days = m / MIN_PER_DAY
        return when {
            m < 60 -> "${m}분"
            m < MIN_PER_DAY -> join(hours, "시간", m % 60, "분")
            days < 30 -> join(days, "일", hours % 24, "시간")
            days < 365 -> join(days / 30, "개월", days % 30, "일")
            else -> join(days / 365, "년", (days % 365) / 30, "개월")
        }
    }

    private fun join(a: Long, au: String, b: Long, bu: String) = if (b == 0L) "$a$au" else "$a$au ${b}$bu"

    private const val MIN_PER_DAY = 60L * 24
}
