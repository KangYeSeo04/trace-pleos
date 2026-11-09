package com.tracers.trace.data.record

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KmRecordDao {
    @Insert
    suspend fun insert(row: KmRecordEntity): Long

    /** 가장 최근 주행의 1km 기록, km 순 */
    @Query(
        "SELECT * FROM km_record WHERE tripStartedAtMs = (SELECT MAX(tripStartedAtMs) FROM km_record) " +
            "ORDER BY kmIndex"
    )
    fun observeLatestTrip(): Flow<List<KmRecordEntity>>

    @Query("DELETE FROM km_record WHERE endedAtMs < :beforeMs")
    suspend fun deleteBefore(beforeMs: Long): Int
}

@Dao
interface CoinWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: CoinWalletEntity)

    @Query("SELECT * FROM coin_wallet WHERE id = 0")
    suspend fun get(): CoinWalletEntity?

    @Query("SELECT * FROM coin_wallet WHERE id = 0")
    fun observe(): Flow<CoinWalletEntity?>
}

@Dao
interface CoinHistoryDao {
    @Insert
    suspend fun insert(row: CoinHistoryEntity): Long

    /** 최신이 앞 */
    @Query("SELECT * FROM coin_history ORDER BY atMs DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CoinHistoryEntity>>

    /** 최근 keep건만 남기고 지운다 */
    @Query(
        "DELETE FROM coin_history WHERE id NOT IN " +
            "(SELECT id FROM coin_history ORDER BY atMs DESC, id DESC LIMIT :keep)"
    )
    suspend fun trimTo(keep: Int): Int
}
