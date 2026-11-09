package com.tracers.trace.data.record

import android.util.Log
import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.BatteryEnergy
import com.tracers.trace.domain.ChargeDetector
import com.tracers.trace.domain.ChargeRecord
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.DriveStateDetector
import com.tracers.trace.domain.TripRecorder
import com.tracers.trace.domain.VehicleSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
 * 주행 중에는 지금까지의 요약을 [SNAPSHOT_INTERVAL_MS]마다 덮어써 두고,
 * 앱이 다시 켜질 때 남아 있으면 그 값(마지막으로 받은 값)으로 주행 1회를 마무리한다.
 *
 * TODO(확인필요: 백그라운드 기록 정책) 앱 프로세스가 죽어 있는 동안의 주행·충전은 기록되지 않는다.
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
    private var job: Job? = null

    /** DB 쓰기를 들어온 순서대로 하나씩 (스냅샷 저장이 주행 마무리 뒤에 끼어들지 않게) */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    /** A-01 주행 상태. 앱 전체(홈 모드, A-02 자동 복귀)가 이 값 하나를 쓴다 */
    val driveState: StateFlow<DriveState> = driveStateDetector.currentState

    fun start() {
        if (job != null) return
        signals.start()

        job = scope.launch {
            // 1) 지난번에 끝나지 않은 주행 마무리 (새 기록보다 먼저)
            runCatching {
                repository.loadInProgress()?.let { snap ->
                    val trip = TripRecorder.finalizeRecovered(snap)
                    repository.finishTrip(trip)
                    Log.i(TAG, "끝나지 않은 주행 마무리: ${trip?.distanceKm ?: "짧아서 버림"}")
                }
                repository.prune(clock())
            }.onFailure { Log.w(TAG, "복구 실패", it) }

            // 2) DB 쓰기 담당 (하나)
            launch {
                for (w in writes) runCatching { w() }.onFailure { Log.w(TAG, "기록 저장 실패", it) }
            }

            // 3) 기어·차속 → 주행 상태
            launch {
                combine(signals.gear, signals.speedMps) { g, v -> g to v }
                    .collect { (g, v) -> driveStateDetector.update(g, v) }
            }

            // 4) 주행 상태 → 주행 시작·종료
            launch {
                driveStateDetector.currentState.collect { st ->
                    val now = clock()
                    val capacity = signals.batteryCapacityWh.value?.let { it / 1000.0 }
                    lock.withLock {
                        val wasRecording = tripRecorder.isRecording
                        val finished = tripRecorder.onDriveState(st, now)
                        when {
                            wasRecording && !tripRecorder.isRecording -> write {
                                repository.finishTrip(finished)
                                Log.i(TAG, "주행 저장: ${finished?.distanceKm ?: "짧아서 버림"}")
                            }
                            !wasRecording && tripRecorder.isRecording -> saveSnapshot()
                        }
                        chargeDetector.onDriveState(st, now, capacity)?.let { saveCharge(it) }
                    }
                }
            }

            // 5) 주행 중 진행 요약을 타이머로 저장 (차량 신호는 바뀔 때만 오기도 해서 샘플에 묶지 않는다)
            launch {
                while (true) {
                    delay(SNAPSHOT_INTERVAL_MS)
                    lock.withLock { if (tripRecorder.isRecording) saveSnapshot() }
                }
            }

            // 6) 차량 값 → 누적
            launch {
                val battery = combine(
                    signals.batteryLevelWh, signals.batteryPercent, signals.batteryCapacityWh,
                ) { wh, pct, cap -> Triple(wh, pct, cap) }
                combine(
                    signals.speedMps, battery, signals.odometerKm, signals.hvacOn, signals.chargePortOpen,
                ) { v, (wh, pct, cap), odo, ac, port ->
                    VehicleSample(
                        timeMs = clock(),
                        speedMps = v,
                        batteryKwh = BatteryEnergy.kwh(wh, pct, cap),
                        batteryPercent = pct,
                        odometerKm = odo?.toDouble(),
                        hvacOn = ac,
                        chargePortOpen = port,
                    ) to cap?.let { it / 1000.0 }
                }.collect { (sample, capacityKwh) ->
                    lock.withLock {
                        tripRecorder.onSample(sample)
                        chargeDetector.onSample(sample, capacityKwh)?.let { saveCharge(it) }
                    }
                }
            }
        }
    }

    private fun saveSnapshot() {
        val snap = tripRecorder.snapshot() ?: return
        write { repository.saveInProgress(snap) }
    }

    private fun saveCharge(c: ChargeRecord) = write {
        repository.saveCharge(c)
        Log.i(TAG, "충전 저장: ${c.startPercent}→${c.endPercent}% ${"%.1f".format(c.addedKwh)}kWh")
    }

    private fun write(block: suspend () -> Unit) {
        writes.trySend(block)
    }

    companion object {
        private const val TAG = "DriveRecordCollector"
        private const val SNAPSHOT_INTERVAL_MS = 30_000L
    }
}
