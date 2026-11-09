package com.tracers.trace.data.record

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 주행 1회 요약 (C-01). 원시 신호는 저장하지 않고 주행이 끝날 때 요약만 남긴다.
 *
 * @property startedAtMs 주행 시작 시각 (epoch ms)
 * @property endedAtMs 주행 종료 시각 (epoch ms)
 * @property distanceKm 총 주행거리 차이로 계산한 이번 주행 거리
 * @property energyUsedKwh 주행 중 배터리 잔량 감소량 합 (회생으로 오른 양은 빼지 않은 순수 소비)
 * @property hardAccelCount 급가속 횟수 (차속 변화로 추정)
 * @property regenKwh 주행 중 배터리 잔량 상승량 합 = 회생제동 추정
 * @property hvacOnSec 주행 중 공조 전원이 켜져 있던 시간 (초)
 * @property startBatteryPercent 시작 시 배터리 %
 * @property endBatteryPercent 종료 시 배터리 %
 */
@Entity(tableName = "trip_summary", indices = [Index("startedAtMs")])
data class TripSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val distanceKm: Double,
    val energyUsedKwh: Double,
    val hardAccelCount: Int,
    val regenKwh: Double,
    val hvacOnSec: Long,
    val startBatteryPercent: Int?,
    val endBatteryPercent: Int?,
)
