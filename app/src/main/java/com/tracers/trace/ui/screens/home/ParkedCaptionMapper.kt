package com.tracers.trace.ui.screens.home

import com.tracers.trace.data.record.EfficiencySnapshot
import com.tracers.trace.domain.ChargeForecast
import com.tracers.trace.domain.MaintenanceItemStatus
import com.tracers.trace.ui.screens.efficiency.EfficiencyUiMapper
import com.tracers.trace.ui.screens.maintenance.MaintenanceUiMapper
import java.time.Instant

/**
 * 정차·주차 중 무대 문구 (B-03). 순수 함수. 우선순위(기획안 B-03 순서):
 * 충전 중 > 점검 임박(D-02) > 충전 타이밍(C-02) > 쉬는 중.
 *
 * 보조 줄: "배터리 N% · 하루 평균 M km" (모르는 값은 뺀다). 충전 타이밍은 추정이라 "· 추정"을 붙인다.
 * 점검 임박의 보조 줄은 "항목 이름 · 남은 값".
 */
object ParkedCaptionMapper {

    /** @param bolt 문구 앞 번개 아이콘 (충전 관련 문구만) */
    data class Caption(val title: String, val sub: String, val bolt: Boolean = false)

    const val MAINTENANCE_TITLE = "점검이 다가와요"

    /**
     * @param imminent 가장 급한 점검 임박 항목 (14일 또는 500 km 이내). 없으면 null
     */
    fun map(charging: Boolean, snapshot: EfficiencySnapshot?, imminent: MaintenanceItemStatus? = null): Caption {
        val sub = subLine(snapshot)
        if (charging) return Caption("충전 중", sub, bolt = true)
        if (imminent != null) {
            return Caption(MAINTENANCE_TITLE, "${imminent.type.displayName} · ${MaintenanceUiMapper.remaining(imminent)}")
        }
        val ready = snapshot?.forecast as? ChargeForecast.Ready
        if (ready != null && ready.days.isNotEmpty()) {
            val today = Instant.ofEpochMilli(snapshot.nowMs).atZone(snapshot.zone).toLocalDate()
            val low = ready.firstLowDate
            val title = when {
                low == null -> "충전은 ${EfficiencyUiMapper.weekdayName(ready.days.last().date.dayOfWeek)}요일까지 여유 있어요"
                low == today -> "오늘 충전하면 좋아요"
                else -> "${EfficiencyUiMapper.weekdayName(low.dayOfWeek)}요일 전에 충전하면 좋아요"
            }
            return Caption(title, if (sub.isEmpty()) "추정" else "$sub 기준 · 추정", bolt = true)
        }
        return Caption("쉬는 중", sub)
    }

    private fun subLine(s: EfficiencySnapshot?): String {
        if (s == null) return ""
        val parts = buildList {
            s.currentPercent?.let { add("배터리 $it%") }
            if (s.trips.isNotEmpty()) {
                val today = Instant.ofEpochMilli(s.nowMs).atZone(s.zone).toLocalDate()
                add("하루 평균 ${EfficiencyUiMapper.dailyAverageKm(s.summary, s.trips, today, s.zone)} km")
            }
        }
        return parts.joinToString(" · ")
    }
}
