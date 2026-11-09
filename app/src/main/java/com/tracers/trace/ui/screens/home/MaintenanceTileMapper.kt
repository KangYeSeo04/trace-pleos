package com.tracers.trace.ui.screens.home

import com.tracers.trace.domain.MaintenanceItemStatus
import com.tracers.trace.domain.MaintenanceLevel
import com.tracers.trace.domain.MaintenanceMeasure
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * 가장 급한 점검 항목 → 홈 정비 타일 (D-01). 순수 함수.
 *
 * - 값: 먼저 오는 쪽 남은 값 (기간이면 일, 거리면 km). 지나면 단위에 "지남"
 * - 상태: 지남 "점검 시기가 지났어요"·경고(≥ 90%) "곧 점검해야 해요"는 warn,
 *   주의(≥ 75%)나 점검 임박(14일·500 km 이내)은 "점검이 다가와요" caution, 그 외 "점검까지 여유로워요" good
 * - 막대: 사용률
 * - 보조 줄: "항목 이름 · 다음 점검 날짜 또는 주행거리" (디자인 "엔진오일 · 2027.09.28")
 */
object MaintenanceTileMapper {

    val Empty = TileState.Maintenance(
        value = "–",
        unit = "일",
        status = "데이터를 모으는 중이에요",
        statusLevel = StatusLevel.Normal,
        barPercent = 0f,
    )

    private val DUE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    /** @param mostUrgent 급한 순 첫 항목. 없으면 빈 타일 */
    fun map(mostUrgent: MaintenanceItemStatus?): TileState.Maintenance {
        val s = mostUrgent ?: return Empty
        val (amount, unit, due) = when (s.leading) {
            MaintenanceMeasure.TIME -> {
                val t = s.time!!
                Triple(t.remainingDays.toDouble(), "일", t.dueDate.format(DUE_DATE))
            }
            MaintenanceMeasure.KM -> {
                val k = s.km!!
                Triple(k.remainingKm, "km", "${thousands(k.dueOdometerKm(s.basisOdometerKm))} km")
            }
            null -> return Empty
        }
        val overdue = amount < 0
        val (status, level) = when {
            overdue -> "점검 시기가 지났어요" to StatusLevel.Warn
            s.level == MaintenanceLevel.WARN -> "곧 점검해야 해요" to StatusLevel.Warn
            s.level == MaintenanceLevel.CAUTION || s.isImminent -> "점검이 다가와요" to StatusLevel.Caution
            else -> "점검까지 여유로워요" to StatusLevel.Good
        }
        return TileState.Maintenance(
            value = thousands(abs(amount)),
            unit = if (overdue) "$unit 지남" else unit,
            status = status,
            statusLevel = level,
            barPercent = (s.usageRatio * 100).coerceIn(0.0, 100.0).toFloat(),
            subText = "${s.type.displayName} · $due",
        )
    }

    private fun thousands(v: Double): String = String.format(Locale.KOREA, "%,d", floor(v).toLong())
}
