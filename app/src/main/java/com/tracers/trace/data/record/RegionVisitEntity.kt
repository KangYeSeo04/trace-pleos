package com.tracers.trace.data.record

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 지역 방문 기록 (G-01·G-03). 161 지역 코드 하나에 한 줄. 위경도 좌표와 시·군·구는 저장하지 않는다.
 */
@Entity(tableName = "region_visit")
data class RegionVisitEntity(
    @PrimaryKey val regionCode: String,
    val firstVisitedAtMs: Long,
    val lastVisitedAtMs: Long,
    val distanceKm: Double,
    val stayMs: Long,
)

@Dao
interface RegionVisitDao {
    /** 처음 온 지역이면 빈 줄을 만든다 (있으면 그대로) */
    @Query(
        "INSERT OR IGNORE INTO region_visit (regionCode, firstVisitedAtMs, lastVisitedAtMs, distanceKm, stayMs) " +
            "VALUES (:code, :atMs, :atMs, 0, 0)"
    )
    suspend fun insertIfAbsent(code: String, atMs: Long)

    /** 증가분 더하기. 마지막 간 시각은 더 늦은 쪽 */
    @Query(
        "UPDATE region_visit SET distanceKm = distanceKm + :distanceKm, stayMs = stayMs + :stayMs, " +
            "lastVisitedAtMs = MAX(lastVisitedAtMs, :atMs) WHERE regionCode = :code"
    )
    suspend fun add(code: String, distanceKm: Double, stayMs: Long, atMs: Long)

    @Query("SELECT * FROM region_visit ORDER BY lastVisitedAtMs DESC")
    fun observeAll(): Flow<List<RegionVisitEntity>>

    @Query("DELETE FROM region_visit WHERE regionCode = :code")
    suspend fun delete(code: String)

    @Query("DELETE FROM region_visit")
    suspend fun deleteAll()
}
