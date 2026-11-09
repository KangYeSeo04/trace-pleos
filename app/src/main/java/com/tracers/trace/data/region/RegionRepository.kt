package com.tracers.trace.data.region

import androidx.room.withTransaction
import com.tracers.trace.data.record.TraceDatabase
import com.tracers.trace.domain.region.RegionVisit
import com.tracers.trace.domain.region.RegionVisitDelta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 지역 방문 기록 저장소 (G-03·G-04). 기기 안에만 저장한다. */
class RegionRepository(private val db: TraceDatabase) {
    private val dao = db.regionVisitDao()

    suspend fun apply(deltas: List<RegionVisitDelta>) {
        if (deltas.isEmpty()) return
        db.withTransaction {
            deltas.forEach { d ->
                dao.insertIfAbsent(d.regionCode, d.atMs)
                dao.add(d.regionCode, d.distanceKm, d.stayMs, d.atMs)
            }
        }
    }

    /** 최근에 간 순 */
    fun observe(): Flow<List<RegionVisit>> = dao.observeAll().map { list ->
        list.map { RegionVisit(it.regionCode, it.firstVisitedAtMs, it.lastVisitedAtMs, it.distanceKm, it.stayMs) }
    }

    suspend fun delete(regionCode: String) = dao.delete(regionCode)

    suspend fun clear() = dao.deleteAll()
}
