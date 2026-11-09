package com.tracers.trace.data.record

import com.tracers.trace.domain.MaintenanceBaseline
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceInputMethod
import com.tracers.trace.domain.MaintenanceItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 정비 기록·기준점 저장소 (D-01·D-04·D-05). 기기 안에만 저장한다 */
class MaintenanceRepository(private val db: TraceDatabase) {

    private val dao get() = db.maintenanceDao()

    suspend fun add(entry: MaintenanceEntry, nowMs: Long): Long = dao.insert(entry.toEntity(nowMs))

    /** 최근 순 */
    fun observeRecords(): Flow<List<MaintenanceEntry>> = dao.observeRecords().map { list -> list.map { it.toDomain() } }

    fun observeBaseline(): Flow<MaintenanceBaseline?> = dao.observeBaseline().map { e ->
        e?.let { MaintenanceBaseline(LocalDate.ofEpochDay(it.epochDay), it.odometerKm) }
    }

    /** 처음 본 날짜·주행거리를 남긴다. 이미 있으면 바꾸지 않는다 */
    suspend fun ensureBaseline(date: LocalDate, odometerKm: Double) =
        dao.insertBaselineIfAbsent(MaintenanceBaselineEntity(epochDay = date.toEpochDay(), odometerKm = odometerKm))

    companion object {
        internal fun MaintenanceEntry.toEntity(nowMs: Long) = MaintenanceRecordEntity(
            id = id,
            epochDay = date.toEpochDay(),
            odometerKm = odometerKm,
            place = place,
            items = items.sortedBy { it.ordinal }.joinToString(",") { it.name },
            other = other,
            costWon = costWon,
            memo = memo,
            inputMethod = method.name,
            createdAtMs = nowMs,
        )

        internal fun MaintenanceRecordEntity.toDomain() = MaintenanceEntry(
            id = id,
            date = LocalDate.ofEpochDay(epochDay),
            odometerKm = odometerKm,
            place = place,
            // 이름이 바뀌어 모르는 값은 버린다
            items = items.split(',').mapNotNull { n -> MaintenanceItemType.entries.firstOrNull { it.name == n } }.toSet(),
            other = other,
            costWon = costWon,
            memo = memo,
            method = runCatching { MaintenanceInputMethod.valueOf(inputMethod) }.getOrDefault(MaintenanceInputMethod.MANUAL),
        )
    }
}
