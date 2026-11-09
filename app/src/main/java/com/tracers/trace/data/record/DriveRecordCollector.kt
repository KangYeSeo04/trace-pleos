package com.tracers.trace.data.record

import android.util.Log
import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.BatteryEnergy
import com.tracers.trace.domain.ChargeDetector
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.DriveStateDetector
import com.tracers.trace.domain.TripRecorder
import com.tracers.trace.domain.VehicleSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 주행·충전 기록 수집 (C-01, C-02). 앱 프로세스가 살아 있는 동안 Application 범위에서 **하나만** 돈다.
 *
 * 차량 신호 → [VehicleSample] → [TripRecorder]·[ChargeDetector] → 요약만 Room에 저장.
 * 원시 신호는 저장하지 않는다.
 *
 * TODO(확인필요: 백그라운드 기록 정책) 앱 프로세스가 죽으면 진행 중이던 주행·충전은 사라진다.
 *  실차에서 상시 백그라운드 기록이 허용되는지 확인한 뒤 서비스 여부를 정한다.
 */
class DriveRecordCollector(
    private val signals: VehicleSignals,
    private val repository: RecordRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val driveStateDetector = DriveStateDetector(scope)
    private val tripRecorder = TripRecorder()
    private val chargeDetector = ChargeDetector()

    /** 두 수집 코루틴이 같은 계산 객체를 건드리므로 순서를 묶는다 */
    private val lock = Mutex()
    private var jobs: List<Job> = emptyList()

    /** A-01 주행 상태. 앱 전체(홈 모드, A-02 자동 복귀)가 이 값 하나를 쓴다 */
    val driveState: StateFlow<DriveState> = driveStateDetector.currentState

    fun start() {
        if (jobs.isNotEmpty()) return
        signals.start()

        val gearSpeed = scope.launch {
            combine(signals.gear, signals.speedMps) { g, v -> g to v }
                .collect { (g, v) -> driveStateDetector.update(g, v) }
        }

        val state = scope.launch {
            driveStateDetector.currentState.collect { st ->
                val now = clock()
                val capacity = signals.batteryCapacityWh.value?.let { it / 1000.0 }
                lock.withLock {
                    tripRecorder.onDriveState(st, now)?.let { save { repository.saveTrip(it) } }
                    chargeDetector.onDriveState(st, now, capacity)?.let { save { repository.saveCharge(it) } }
                }
            }
        }

        val samples = scope.launch {
            val battery = combine(
                signals.batteryLevelWh, signals.batteryPercent, signals.batteryCapacityWh,
            ) { wh, pct, cap -> Triple(wh, pct, cap) }
            combine(
                signals.speedMps, battery, signals.odometerKm, signals.hvacAcOn, signals.chargePortOpen,
            ) { v, (wh, pct, cap), odo, ac, port ->
                VehicleSample(
                    timeMs = clock(),
                    speedMps = v,
                    batteryKwh = BatteryEnergy.kwh(wh, pct, cap),
                    batteryPercent = pct,
                    odometerKm = odo?.toDouble(),
                    hvacAcOn = ac,
                    chargePortOpen = port,
                ) to cap?.let { it / 1000.0 }
            }.collect { (sample, capacityKwh) ->
                lock.withLock {
                    tripRecorder.onSample(sample)
                    chargeDetector.onSample(sample, capacityKwh)?.let { save { repository.saveCharge(it) } }
                }
            }
        }

        scope.launch { runCatching { repository.prune(clock()) } }
        jobs = listOf(gearSpeed, state, samples)
    }

    private fun save(block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure { Log.w(TAG, "기록 저장 실패", it) }
        }
    }

    companion object {
        private const val TAG = "DriveRecordCollector"
    }
}
