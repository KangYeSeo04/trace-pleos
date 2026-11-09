package com.tracers.trace.ui.screens.maintenance

import com.tracers.trace.domain.MaintenanceBasis
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceInputMethod
import com.tracers.trace.domain.MaintenanceItemStatus
import com.tracers.trace.domain.MaintenanceItemType
import com.tracers.trace.domain.MaintenanceMeasure
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/** domain 정비 계산 결과 → 정비 화면 글자. 순수 함수 */
object MaintenanceUiMapper {

    const val OTHER_CHIP = "기타"
    const val FIRST_SEEN_NOTE = "기준: 처음 기록일"

    /** 기록 추가 양식 항목 칩: 점검 항목 8개 + 기타 */
    val CHIP_OPTIONS: List<String> = MaintenanceItemType.entries.map { it.chipLabel } + OTHER_CHIP

    fun item(s: MaintenanceItemStatus): MaintenanceItem {
        val kmGauge = s.type.intervalKm?.let { interval ->
            val km = s.km
            if (km == null) UsageGauge(0f, "– / ${thousands(interval)} km")
            else UsageGauge(percent(km.ratio), "${thousands(floor(km.usedKm).toLong())} / ${thousands(interval)} km")
        }
        val timeGauge = s.time?.let { t -> UsageGauge(percent(t.ratio), "${months(t.elapsedMonths)} / ${t.intervalMonths}개월") }
        // 먼저 오는 쪽 막대가 위
        val gauges = if (s.leading == MaintenanceMeasure.TIME) listOfNotNull(timeGauge, kmGauge) else listOfNotNull(kmGauge, timeGauge)
        return MaintenanceItem(
            name = s.type.displayName,
            remaining = remaining(s),
            gauges = gauges,
            basisNote = if (s.basis == MaintenanceBasis.FIRST_SEEN) FIRST_SEEN_NOTE else null,
        )
    }

    /** "1,200 km 남음", "12일 남음", "7개월 남음", "약 2년 남음", 지나면 "N일 지남" */
    fun remaining(s: MaintenanceItemStatus): String = when (s.leading) {
        MaintenanceMeasure.KM -> {
            val r = s.km!!.remainingKm
            if (r >= 0) "${thousands(floor(r).toLong())} km 남음" else "${thousands(floor(-r).toLong())} km 지남"
        }
        MaintenanceMeasure.TIME -> days(s.time!!.remainingDays)
        null -> "주행거리 확인 중"
    }

    /** 남은 일수 → 글자. 60일까지 일, 2년 미만 개월, 그 이상 년 */
    fun days(d: Long): String = when {
        d < 0 -> "${abs(d)}일 지남"
        d <= 60 -> "${d}일 남음"
        d < 730 -> "${(d / DAYS_PER_MONTH).roundToLong()}개월 남음"
        else -> "약 ${(d / DAYS_PER_YEAR).roundToLong()}년 남음"
    }

    fun record(e: MaintenanceEntry): MaintenanceRecord {
        val names = e.items.sortedBy { it.ordinal }.map { it.displayName }
        return MaintenanceRecord(
            id = e.id,
            date = e.date,
            odometerKm = e.odometerKm,
            title = (names + listOfNotNull(OTHER_CHIP.takeIf { e.other })).joinToString(" · "),
            place = e.place,
            method = if (e.method == MaintenanceInputMethod.AUTO_DETECTED) RecordInputMethod.AutoDetected else RecordInputMethod.Manual,
            itemNames = names,
            costWon = e.costWon,
        )
    }

    private fun percent(ratio: Double): Float = (ratio * 100).coerceIn(0.0, 100.0).toFloat()

    /** 11.6 → "11.6", 17.0 → "17" */
    private fun months(m: Double): String {
        val r = (m * 10).roundToLong() / 10.0
        return if (r % 1.0 == 0.0) r.toLong().toString() else String.format(Locale.KOREA, "%.1f", r)
    }

    private fun thousands(v: Number): String = String.format(Locale.KOREA, "%,d", v.toLong())

    private const val DAYS_PER_MONTH = 365.25 / 12
    private const val DAYS_PER_YEAR = 365.25
}
