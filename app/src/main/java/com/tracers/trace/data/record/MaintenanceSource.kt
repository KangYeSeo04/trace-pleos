package com.tracers.trace.data.record

import android.util.Log
import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.MaintenanceCalculator
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceItemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor

/** 정비 계산 한 번의 결과 (홈 타일·무대 문구와 정비 화면이 같이 쓴다) */
data class MaintenanceSnapshot(
    /** 급한 순 */
    val statuses: List<MaintenanceItemStatus>,
    /** 최근 순 */
    val records: List<MaintenanceEntry>,
    /** 지금 총 주행거리 (km). 모르면 null */
    val odometerKm: Double?,
    val today: LocalDate,
)

/**
 * 정비 기록 + 기준점 + 총 주행거리(Vehicle SDK `Odometer.getCurrentDistanceDriven`) → [MaintenanceCalculator].
 * AAOS PERF_ODOMETER는 권한 오류라 쓰지 않는다 (docs/PLEOS_API_확인현황.md).
 *
 * TODO(A-06): 주행거리 미지원 차종은 차속 적분 + 최초 설정 시 계기판 입력 (D-01 대체 계산)
 */
class MaintenanceSource(
    private val repository: MaintenanceRepository,
    private val signals: VehicleSignals,
    private val scope: CoroutineScope,
    private val today: () -> LocalDate = LocalDate::now,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private var started = false

    /** 화면이 다시 보일 때(onResume) 다시 계산하려고 올리는 값 */
    private val refreshTick = MutableStateFlow(0)

    /** 날짜가 바뀌었을 수 있어 다시 계산한다 (Activity onResume에서 부른다) */
    fun refresh() = refreshTick.update { it + 1 }

    /** 지금 날짜를 내보내고, 자정이 지날 때마다 다시 내보낸다 */
    private fun midnights(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(msUntilNextMidnight(clock(), zone()) + MIDNIGHT_SLACK_MS)
        }
    }

    /** 처음 받은 총 주행거리를 기준점으로 남긴다 (이미 있으면 그대로). 차량 신호 구독 뒤에 부른다 */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            // 연결 직후 0이 먼저 올 수 있어 0보다 큰 값을 잠깐 기다린다. 끝내 0이면(새 차·에뮬레이터) 0을 쓴다
            val odo = (withTimeoutOrNull(NONZERO_WAIT_MS) { signals.odometerKm.first { it != null && it > 0f } }
                ?: signals.odometerKm.filterNotNull().first()).toDouble()
            runCatching { repository.ensureBaseline(today(), odo) }
                .onFailure { Log.w(TAG, "정비 기준점 저장 실패", it) }
        }
    }

    fun observe(): Flow<MaintenanceSnapshot> {
        // 주행 중 주행거리가 자주 바뀌므로 0.1 km 단위로 줄여 바뀔 때만 다시 계산
        val odometer = signals.odometerKm
            .map { v -> v?.takeIf { it >= 0f }?.let { floor(it * 10.0) / 10.0 } }
            .distinctUntilChanged()
        // 자정·onResume마다 같은 기록으로 다시 계산 (남은 기간이 날짜에 따라 바뀐다)
        val ticks = combine(refreshTick, midnights()) { _, _ -> }
        return combine(repository.observeRecords(), repository.observeBaseline(), odometer, ticks) { records, base, odo, _ ->
            val d = today()
            MaintenanceSnapshot(MaintenanceCalculator.calculate(records, base, d, odo), records, odo, d)
        }.flowOn(Dispatchers.Default)
    }

    suspend fun add(entry: MaintenanceEntry): Long = repository.add(entry, clock())

    companion object {
        private const val TAG = "MaintenanceSource"
        private const val NONZERO_WAIT_MS = 10_000L
        /** 자정 직전에 깨어 같은 날짜로 계산하지 않도록 1초 늦게 */
        private const val MIDNIGHT_SLACK_MS = 1_000L

        /** 다음 자정까지 남은 ms */
        internal fun msUntilNextMidnight(nowMs: Long, zone: ZoneId): Long {
            val now = Instant.ofEpochMilli(nowMs).atZone(zone)
            val next = now.toLocalDate().plusDays(1).atStartOfDay(zone)
            return (next.toInstant().toEpochMilli() - nowMs).coerceAtLeast(0)
        }
    }
}
