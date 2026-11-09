package com.tracers.trace.data.record

import androidx.room.withTransaction
import com.tracers.trace.domain.ChargeRecord
import com.tracers.trace.domain.TripRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 주행·충전 요약 저장소. domain 모델 ↔ Room 엔티티 변환만 한다. */
class RecordRepository(private val db: TraceDatabase) {

    /** 주행 1회 저장 + 진행 중 요약 삭제를 한 번에 */
    suspend fun finishTrip(t: TripRecord?) {
        db.withTransaction {
            if (t != null) db.tripSummaryDao().insert(t.toEntity())
            db.tripInProgressDao().clear()
        }
    }

    /** 진행 중 주행의 지금까지 요약을 덮어쓴다 */
    suspend fun saveInProgress(t: TripRecord) {
        db.tripInProgressDao().upsert(
            TripInProgressEntity(
                startedAtMs = t.startedAtMs, endedAtMs = t.endedAtMs, distanceKm = t.distanceKm,
                energyUsedKwh = t.energyUsedKwh, hardAccelCount = t.hardAccelCount,
                regenKwh = t.regenKwh, hvacOnSec = t.hvacOnSec,
                startBatteryPercent = t.startBatteryPercent, endBatteryPercent = t.endBatteryPercent,
            )
        )
    }

    /** 앱 시작 시 남아 있는 진행 중 요약을 꺼낸다 (없으면 null) */
    suspend fun loadInProgress(): TripRecord? = db.tripInProgressDao().get()?.let {
        TripRecord(
            startedAtMs = it.startedAtMs, endedAtMs = it.endedAtMs, distanceKm = it.distanceKm,
            energyUsedKwh = it.energyUsedKwh, hardAccelCount = it.hardAccelCount,
            regenKwh = it.regenKwh, hvacOnSec = it.hvacOnSec,
            startBatteryPercent = it.startBatteryPercent, endBatteryPercent = it.endBatteryPercent,
        )
    }

    private fun TripRecord.toEntity() = TripSummaryEntity(
        startedAtMs = startedAtMs, endedAtMs = endedAtMs, distanceKm = distanceKm,
        energyUsedKwh = energyUsedKwh, hardAccelCount = hardAccelCount,
        regenKwh = regenKwh, hvacOnSec = hvacOnSec,
        startBatteryPercent = startBatteryPercent, endBatteryPercent = endBatteryPercent,
    )

    /** @param placeLabel 충전 장소 = 충전이 끝난 때의 지역 이름 ("서울 중랑구"). 모르면 null */
    suspend fun saveCharge(c: ChargeRecord, placeLabel: String? = null) {
        db.chargeSessionDao().insert(
            ChargeSessionEntity(
                startedAtMs = c.startedAtMs, endedAtMs = c.endedAtMs,
                startPercent = c.startPercent, endPercent = c.endPercent, addedKwh = c.addedKwh,
                placeLabel = placeLabel,
            )
        )
    }

    fun observeTrips(sinceMs: Long): Flow<List<TripRecord>> =
        db.tripSummaryDao().observeSince(sinceMs).map { list ->
            list.map {
                TripRecord(
                    startedAtMs = it.startedAtMs, endedAtMs = it.endedAtMs, distanceKm = it.distanceKm,
                    energyUsedKwh = it.energyUsedKwh, hardAccelCount = it.hardAccelCount,
                    regenKwh = it.regenKwh, hvacOnSec = it.hvacOnSec,
                    startBatteryPercent = it.startBatteryPercent, endBatteryPercent = it.endBatteryPercent,
                )
            }
        }

    fun observeCharges(sinceMs: Long): Flow<List<ChargeRecord>> =
        db.chargeSessionDao().observeSince(sinceMs).map { list ->
            list.map { ChargeRecord(it.startedAtMs, it.endedAtMs, it.startPercent, it.endPercent, it.addedKwh) }
        }

    /** 보관 기간(기본 100일)이 지난 요약 삭제. 90일 평균 + 여유 */
    suspend fun prune(nowMs: Long, keepDays: Int = 100) {
        val before = nowMs - keepDays * 86_400_000L
        db.tripSummaryDao().deleteBefore(before)
        db.chargeSessionDao().deleteBefore(before)
    }
}
