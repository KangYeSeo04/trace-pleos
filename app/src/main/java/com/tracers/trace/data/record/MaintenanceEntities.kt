package com.tracers.trace.data.record

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 정비 기록 (D-04·D-05).
 *
 * @param epochDay 정비한 날 (LocalDate.toEpochDay)
 * @param items 점검 항목 이름(MaintenanceItemType.name)을 쉼표로 이은 값. "기타"는 [other]
 * @param inputMethod MaintenanceInputMethod.name
 */
@Entity(tableName = "maintenance_record", indices = [Index("epochDay")])
data class MaintenanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val odometerKm: Int,
    val place: String,
    val items: String,
    val other: Boolean,
    val costWon: Int?,
    val memo: String,
    val inputMethod: String,
    val createdAtMs: Long,
)

/** 앱이 처음 본 날짜·총 주행거리 (한 줄, id = 0). 기록이 없는 항목의 기준 */
@Entity(tableName = "maintenance_baseline")
data class MaintenanceBaselineEntity(
    @PrimaryKey val id: Int = 0,
    val epochDay: Long,
    val odometerKm: Double,
)

@Dao
interface MaintenanceDao {
    @Insert
    suspend fun insert(record: MaintenanceRecordEntity): Long

    /** 최근 순 */
    @Query("SELECT * FROM maintenance_record ORDER BY epochDay DESC, id DESC")
    fun observeRecords(): Flow<List<MaintenanceRecordEntity>>

    /** 이미 있으면 그대로 둔다 (처음 본 값만 남긴다) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBaselineIfAbsent(baseline: MaintenanceBaselineEntity)

    @Query("SELECT * FROM maintenance_baseline WHERE id = 0")
    fun observeBaseline(): Flow<MaintenanceBaselineEntity?>
}
