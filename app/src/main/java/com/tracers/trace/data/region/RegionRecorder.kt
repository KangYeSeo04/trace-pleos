package com.tracers.trace.data.region

import android.util.Log
import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.SpeedSpikeFilter
import com.tracers.trace.domain.region.RegionLocator
import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisitTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 지금 지역 판정과 방문 기록 (G-01·G-03·G-04). Application 범위에서 하나만 돈다.
 *
 * 위치([VehicleSignals.location]) → [RegionLocator] → [RegionVisitTracker] → 지역별 합만 Room에 저장.
 * 위경도는 판정 직후 버리고, 구 이름은 [status]에만 둔다.
 *
 * - 저장: 지역이 바뀔 때, 주차할 때, 그 밖에는 [FLUSH_INTERVAL_MS]마다
 * - 위치 기록이 꺼지면([recordingOn] false) 쌓인 값을 저장하고 새 기록을 멈춘다 (쌓인 기록은 그대로)
 * - 위치가 [staleAfterMs] 넘게 안 오면 [RegionStatus.Unavailable]
 */
class RegionRecorder(
    private val signals: VehicleSignals,
    private val driveState: StateFlow<DriveState>,
    private val recordingOn: StateFlow<Boolean>,
    private val repository: RegionRepository,
    private val loadData: suspend () -> RegionLookupData,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val tracker = RegionVisitTracker()
    private val spikeFilter = SpeedSpikeFilter()
    private var locator: RegionLocator? = null

    /** tracker·상태를 같이 바꾸는 구간 */
    private val lock = Mutex()
    /** DB 쓰기·지우기 순서 (지운 뒤에 옛 증가분이 다시 써지지 않게) */
    private val writeLock = Mutex()

    private var job: Job? = null
    private var lastValidFixMs: Long? = null
    private var searchingSinceMs = 0L
    private var lastFlushMs = 0L

    private val _status = MutableStateFlow<RegionStatus>(
        if (recordingOn.value) RegionStatus.Searching else RegionStatus.Off
    )
    val status: StateFlow<RegionStatus> = _status.asStateFlow()

    private val _data = MutableStateFlow<RegionLookupData?>(null)
    /** 161 지역 이름·시·도 (화면 표시용). 읽기 전 null */
    val data: StateFlow<RegionLookupData?> = _data.asStateFlow()

    /** 충전 장소 등에 붙일 지금 지역 이름 ("서울 중랑구"). 모르거나 기록이 꺼져 있으면 null */
    val currentLabel: String? get() = (status.value as? RegionStatus.Located)?.match?.label

    fun start() {
        if (job != null) return
        job = scope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            val d = runCatching { withContext(Dispatchers.IO) { loadData() } }
                .onFailure { Log.e(TAG, "경계 데이터 읽기 실패", it) }
                .getOrNull()
            if (d == null) {
                _status.value = RegionStatus.Unavailable
                return@launch
            }
            Log.i(TAG, "경계 데이터 ${d.units.size}개 읽음 (${android.os.SystemClock.elapsedRealtime() - t0} ms)")
            locator = RegionLocator(d)
            _data.value = d
            searchingSinceMs = clock()

            // 1) 위치 기록 켜기·끄기
            launch {
                recordingOn.collect { on ->
                    if (!on) {
                        flush()
                        lock.withLock {
                            tracker.reset()
                            lastValidFixMs = null
                            _status.value = RegionStatus.Off
                        }
                        Log.i(TAG, "위치 기록 꺼짐")
                    } else if (_status.value == RegionStatus.Off) {
                        lock.withLock {
                            searchingSinceMs = clock()
                            _status.value = RegionStatus.Searching
                        }
                        Log.i(TAG, "위치 기록 켜짐")
                    }
                }
            }

            // 2) 위치 → 지역 판정
            launch {
                signals.location.filterNotNull().collect { fix ->
                    if (!recordingOn.value) return@collect
                    if (!fix.isValid) return@collect // 내비가 아직 위치를 못 잡음: 오래 이어지면 3)에서 Unavailable
                    val changed = lock.withLock {
                        val m = locator?.locate(fix.latitude, fix.longitude)
                        val prev = tracker.currentRegion
                        tracker.onRegion(m?.regionCode, fix.timeMs)
                        lastValidFixMs = fix.timeMs
                        _status.value = m?.let { RegionStatus.Located(it) } ?: RegionStatus.Outside
                        if (prev != m?.regionCode) Log.i(TAG, "지역 ${prev ?: "-"} → ${m?.regionCode ?: "지도 밖"}")
                        prev != m?.regionCode
                    }
                    if (changed) flush()
                }
            }

            // 3) 오래 안 오는 위치 확인 + 주기 저장
            launch {
                while (true) {
                    delay(CHECK_INTERVAL_MS)
                    val now = clock()
                    lock.withLock {
                        if (!recordingOn.value) return@withLock
                        val since = lastValidFixMs ?: searchingSinceMs
                        if (now - since > staleAfterMs(driveState.value) && _status.value != RegionStatus.Unavailable) {
                            tracker.onRegion(null, now)
                            _status.value = RegionStatus.Unavailable
                            Log.w(TAG, "위치를 받지 못함")
                        }
                    }
                    if (now - lastFlushMs >= FLUSH_INTERVAL_MS) flush()
                }
            }

            // 4) 차속 → 지역별 달린 거리
            launch {
                signals.speedReadings.collect { r ->
                    lock.withLock {
                        val ok = spikeFilter.push(r)
                        if (recordingOn.value) ok.forEach(tracker::onSpeed)
                    }
                }
            }

            // 5) 주차하면 저장
            launch {
                driveState.collect { if (it == DriveState.PARKED) flush() }
            }
        }
    }

    /** 지역 하나 지우기 (G-04). 아직 저장 안 한 그 지역 증가분도 버린다 */
    suspend fun delete(regionCode: String) = writeLock.withLock {
        lock.withLock { tracker.discard(regionCode) }
        repository.delete(regionCode)
        Log.i(TAG, "지역 기록 지움: $regionCode")
    }

    /** 전체 지우기 (G-04) */
    suspend fun clearAll() = writeLock.withLock {
        lock.withLock { tracker.discard(null) }
        repository.clear()
        Log.i(TAG, "지역 기록 모두 지움")
    }

    private suspend fun flush() = writeLock.withLock {
        val deltas = lock.withLock { tracker.drain() }
        lastFlushMs = clock()
        runCatching { repository.apply(deltas) }.onFailure { Log.w(TAG, "지역 기록 저장 실패", it) }
    }

    companion object {
        private const val TAG = "RegionRecorder"
        private const val CHECK_INTERVAL_MS = 5_000L
        private const val FLUSH_INTERVAL_MS = 30_000L

        /** 요청 3번 넘게 답이 없으면 못 받는 것으로 본다 */
        fun staleAfterMs(state: DriveState): Long = LocationPoller.intervalFor(state) * 3 + 5_000L
    }
}
