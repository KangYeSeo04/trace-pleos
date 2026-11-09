package com.tracers.trace.data.record

import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.ChargeForecast
import com.tracers.trace.domain.ChargeForecaster
import com.tracers.trace.domain.ChargeRecord
import com.tracers.trace.domain.EfficiencyCalculator
import com.tracers.trace.domain.EfficiencySummary
import com.tracers.trace.domain.TripRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.ZoneId

/** 전비 계산 한 번의 결과 (홈 타일과 전비 화면이 같이 쓴다) */
data class EfficiencySnapshot(
    val summary: EfficiencySummary,
    val forecast: ChargeForecast,
    val trips: List<TripRecord>,
    val charges: List<ChargeRecord>,
    val currentPercent: Int?,
    val nowMs: Long,
    val zone: ZoneId,
)

/**
 * Room 기록 + 현재 배터리 → domain 계산 → [EfficiencySnapshot]. 기록이 바뀌거나 배터리 %가 바뀔 때마다 다시 계산.
 */
class EfficiencySource(
    private val repository: RecordRepository,
    private val signals: VehicleSignals,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    fun observe(): Flow<EfficiencySnapshot> {
        val since = clock() - EfficiencyCalculator.WINDOW_DAYS * DAY_MS
        return combine(
            repository.observeTrips(since),
            repository.observeCharges(since),
            signals.batteryPercent,
            signals.batteryCapacityWh,
        ) { trips, charges, pct, capWh ->
            val now = clock()
            val z = zone()
            val summary = EfficiencyCalculator(z).summarize(trips, charges, now)
            val forecast = ChargeForecaster(z).forecast(
                currentPercent = pct,
                capacityKwh = capWh?.let { it / 1000.0 },
                kmPerKwh = summary.averageKmPerKwh,
                trips = trips,
                nowMs = now,
            )
            EfficiencySnapshot(summary, forecast, trips, charges, pct, now, z)
        }.flowOn(Dispatchers.Default)
    }

    private companion object {
        const val DAY_MS = 86_400_000L
    }
}
