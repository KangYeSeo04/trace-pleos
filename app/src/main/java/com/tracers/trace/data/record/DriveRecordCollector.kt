package com.tracers.trace.data.record

import android.util.Log
import com.tracers.trace.data.vehicle.Gear
import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.BatteryEnergy
import com.tracers.trace.domain.ChargeDetector
import com.tracers.trace.domain.ChargeRecord
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.DriveStateDetector
import com.tracers.trace.domain.DrivingEvent
import com.tracers.trace.domain.DrivingEventDetector
import com.tracers.trace.domain.KmSegmenter
import com.tracers.trace.domain.RoughDrivingWindow
import com.tracers.trace.domain.SpeedReading
import com.tracers.trace.domain.SpeedSpikeFilter
import com.tracers.trace.domain.TripRecorder
import com.tracers.trace.domain.VehicleSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 주행·충전·안전점수 기록 수집 (C-01, C-02, B-04, E-01~E-03). 앱 프로세스가 살아 있는 동안 Application 범위에서 **하나만** 돈다.
 *
 * 차량 신호 → [VehicleSample] → [TripRecorder]·[ChargeDetector] → 요약만 Room에 저장.
 * 원시 신호는 저장하지 않는다.
 *
 * 차속 하나하나 → [SpeedSpikeFilter] → [DrivingEventDetector] → 이벤트를 전비(급가속)·1km 구간·거칠었음에 나눠 준다.
 * 1km가 차면 [CoinRepository.applyKm]으로 안전점수·코인을 갱신한다. 주행 상태 판정은 [driveState] 하나다.
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
    private val coinRepository: CoinRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val driveStateDetector = DriveStateDetector(scope)
    private val tripRecorder = TripRecorder()
    private val chargeDetector = ChargeDetector()
    private val spikeFilter = SpeedSpikeFilter()
    private val eventDetector = DrivingEventDetector()
    private val kmSegmenter = KmSegmenter()
    private val roughWindow = RoughDrivingWindow()
    private val _roughDriving = MutableStateFlow(false)
    private val _charging = MutableStateFlow(false)

    /** 두 수집 코루틴이 같은 계산 객체를 건드리므로 순서를 묶는다 */
    private val lock = Mutex()
    private var job: Job? = null

    /** DB 쓰기를 들어온 순서대로 하나씩 (스냅샷 저장이 주행 마무리 뒤에 끼어들지 않게) */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    /** A-01 주행 상태. 앱 전체(홈 모드, A-02 자동 복귀)가 이 값 하나를 쓴다 */
    val driveState: StateFlow<DriveState> = driveStateDetector.currentState

    /** B-04 거칠었음: 급가속·급감속이 최근 2분에 2회 이상 */
    val roughDriving: StateFlow<Boolean> = _roughDriving.asStateFlow()

    /** B-03 충전 중: 충전구가 열려 충전을 세는 중 */
    val charging: StateFlow<Boolean> = _charging.asStateFlow()

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
                coinRepository.prune(clock())
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
                        _charging.value = chargeDetector.isCharging
                        // 주차하면 덜 찬 1km는 점수 없이 버리고 다음 주행은 1km부터
                        if (st == DriveState.PARKED) kmSegmenter.reset()
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

            // 6) 차속 하나하나 → 잡음 거르기 → 위험 운전 이벤트 (값을 건너뛰지 않는 흐름)
            launch {
                signals.speedReadings.collect { r ->
                    lock.withLock { spikeFilter.push(r).forEach(::onSpeed) }
                }
            }

            // 7) 제한속도 → 과속 판정
            launch {
                signals.speedLimitKmh.collect { kmh -> lock.withLock { eventDetector.onSpeedLimit(kmh) } }
            }

            // 8) 차량 값 → 누적
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
                        _charging.value = chargeDetector.isCharging
                    }
                }
            }
        }
    }

    /** 걸러진 차속 하나. lock 안에서 부른다 */
    private fun onSpeed(r: SpeedReading) {
        // 이벤트를 먼저 붙이고 구간을 닫는다 (경계 순간의 이벤트는 그 km에)
        eventDetector.onSpeed(r).forEach(::onEvent)
        kmSegmenter.onSpeed(r)?.let { seg ->
            val tripStart = tripRecorder.currentStartMs ?: seg.startedAtMs
            write {
                val res = coinRepository.applyKm(tripStart, seg)
                Log.i(TAG, "1km #${seg.index}: ${res.kmScore}점, 안전점수 ${"%.2f".format(res.scoreAfter)}, +${res.coinsEarned}코인")
            }
        }
        _roughDriving.value = roughWindow.isRough(r.timeMs)
    }

    private fun onEvent(e: DrivingEvent) {
        // P단에서 튀는 차속(콘솔 실측: 주차 직후 0→48→0)은 운전이 아니다. 다음 주행 첫 1km에 섞이지 않게 버린다
        if (signals.gear.value == Gear.PARK) {
            Log.i(TAG, "이벤트 ${e.type} 무시 (P단)")
            return
        }
        Log.i(TAG, "이벤트 ${e.type}")
        tripRecorder.onDrivingEvent(e)
        kmSegmenter.onEvent(e)
        roughWindow.onEvent(e)
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
