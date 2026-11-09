package com.tracers.trace.data.record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripSummaryDao {
    @Insert
    suspend fun insert(trip: TripSummaryEntity): Long

    /** sinceMs 이후 시작한 주행, 오래된 순 */
    @Query("SELECT * FROM trip_summary WHERE startedAtMs >= :sinceMs ORDER BY startedAtMs")
    fun observeSince(sinceMs: Long): Flow<List<TripSummaryEntity>>

    /** 보관 기간이 지난 요약 정리 (90일 평균에 필요한 만큼만 남김) */
    @Query("DELETE FROM trip_summary WHERE startedAtMs < :beforeMs")
    suspend fun deleteBefore(beforeMs: Long): Int
}

@Dao
interface ChargeSessionDao {
    @Insert
    suspend fun insert(session: ChargeSessionEntity): Long

    @Query("SELECT * FROM charge_session WHERE startedAtMs >= :sinceMs ORDER BY startedAtMs")
    fun observeSince(sinceMs: Long): Flow<List<ChargeSessionEntity>>

    @Query("DELETE FROM charge_session WHERE startedAtMs < :beforeMs")
    suspend fun deleteBefore(beforeMs: Long): Int
}
