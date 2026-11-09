package com.tracers.trace.data.record

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 진행 중인 주행의 지금까지 요약 (한 줄만, id = 0). 원시 신호가 아니라 누적 요약이다.
 * 앱이 주행 도중 꺼지면 다음 시작 때 이 값으로 주행 1회를 마무리하고 지운다.
 */
@Entity(tableName = "trip_in_progress")
data class TripInProgressEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val distanceKm: Double,
    val energyUsedKwh: Double,
    val hardAccelCount: Int,
    val regenKwh: Double,
    val hvacOnSec: Long,
    val startBatteryPercent: Int?,
    val endBatteryPercent: Int?,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
