package com.tracers.trace.data.record

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 충전 1회 기록 (C-01 충전 횟수, C-02).
 *
 * @property addedKwh 충전된 양. 잔량(Wh) 차이, 없으면 용량 × % 차이로 계산
 * @property placeLabel 충전 장소 = 충전이 끝난 때의 지역 이름 (G-01, "서울 중랑구"). 위치를 모르거나 기록이 꺼져 있으면 null
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
