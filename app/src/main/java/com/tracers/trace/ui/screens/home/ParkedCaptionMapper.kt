package com.tracers.trace.ui.screens.home

import com.tracers.trace.data.record.EfficiencySnapshot
import com.tracers.trace.domain.ChargeForecast
import com.tracers.trace.ui.screens.efficiency.EfficiencyUiMapper
import java.time.Instant

/**
 * 정차·주차 중 무대 문구 (B-03). 순수 함수. 우선순위(기획안 B-03 순서):
 * 충전 중 > 점검 임박(TODO D-02) > 충전 타이밍(C-02) > 쉬는 중.
 *
 * 보조 줄: "배터리 N% · 하루 평균 M km" (모르는 값은 뺀다). 충전 타이밍은 추정이라 "· 추정"을 붙인다.
 */
object ParkedCaptionMapper {

    data class Caption(val title: String, val sub: String)

    fun map(charging: Boolean, snapshot: EfficiencySnapshot?): Caption {
        val sub = subLine(snapshot)
        if (charging) return Caption("충전 중", sub)
        // TODO(D-02): 점검 임박(14일 또는 500 km 이내) 문구가 여기 들어간다
        val ready = snapshot?.forecast as? ChargeForecast.Ready
        if (ready != null && ready.days.isNotEmpty()) {
            val today = Instant.ofEpochMilli(snapshot.nowMs).atZone(snapshot.zone).toLocalDate()
            val low = ready.firstLowDate
            val title = when {
                low == null -> "충전은 ${EfficiencyUiMapper.weekdayName(ready.days.last().date.dayOfWeek)}요일까지 여유 있어요"
                low == today -> "오늘 충전하면 좋아요"
                else -> "${EfficiencyUiMapper.weekdayName(low.dayOfWeek)}요일 전에 충전하면 좋아요"
            }
            return Caption(title, if (sub.isEmpty()) "추정" else "$sub 기준 · 추정")
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
