package com.tracers.trace.data.record

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1km 기록 한 줄 (E-03 이번 주행 격자, 안전점수 꺾은선). 원시 차속이 아니라 구간 요약이다.
 *
 * @property tripStartedAtMs 이 km가 속한 주행의 시작 시각. 같은 값끼리 한 주행
 * @property kmIndex 주행 안에서 몇 번째 km (1부터)
 * @property kmScore 이번 1km 점수 (0~100)
 * @property scoreBefore 이 km를 반영하기 전 안전점수
 * @property scoreAfter 반영한 뒤 안전점수
 * @property coinsEarned 이 km로 적립한 코인 (0 또는 1)
 */
@Entity(tableName = "km_record", indices = [Index("tripStartedAtMs")])
data class KmRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripStartedAtMs: Long,
    val kmIndex: Int,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val hardAccel: Int,
    val hardDecel: Int,
    val hardStop: Int,
    val speeding: Int,
    val kmScore: Int,
    val scoreBefore: Double,
    val scoreAfter: Double,
    val coinsEarned: Int,
)

/** 안전점수·받지 않은 코인·보유 코인 (한 줄만, id = 0) */
@Entity(tableName = "coin_wallet")
data class CoinWalletEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val safetyScore: Double,
    val unclaimedCoins: Int,
    val balanceCoins: Int,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

/**
 * 코인 기록 한 줄 (최근 30건만 남긴다).
 *
 * @property kind "EARN" 또는 "SPEND" (CoinHistoryKind 이름)
 * @property amount 받은 코인은 양수, 쓴 코인은 음수
 * @property note 쓴 경우 아이템 id
 */
@Entity(tableName = "coin_history", indices = [Index("atMs")])
data class CoinHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atMs: Long,
    val kind: String,
    val amount: Int,
    val note: String?,
)
