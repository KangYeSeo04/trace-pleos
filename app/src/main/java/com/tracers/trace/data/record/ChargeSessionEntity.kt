package com.tracers.trace.data.record

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 충전 1회 기록 (C-01 충전 횟수, C-02).
 *
 * @property addedKwh 충전된 양. 잔량(Wh) 차이, 없으면 용량 × % 차이로 계산
 * @property placeLabel 충전 장소. TODO(G-01): 지역 판정 붙이면 채움. 지금은 항상 null
 */
@Entity(tableName = "charge_session", indices = [Index("startedAtMs")])
data class ChargeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val startPercent: Int,
    val endPercent: Int,
    val addedKwh: Double,
    val placeLabel: String? = null,
)
