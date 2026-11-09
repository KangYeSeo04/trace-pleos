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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
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
) {
    private var started = false

    /** 처음 받은 총 주행거리를 기준점으로 남긴다 (이미 있으면 그대로). 차량 신호 구독 뒤에 부른다 */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            // 연결 직후 0이 먼저 올 수 있어 0보다 큰 첫 값을 쓴다
            val odo = signals.odometerKm.first { it != null && it > 0f }!!.toDouble()
            runCatching { repository.ensureBaseline(today(), odo) }
                .onFailure { Log.w(TAG, "정비 기준점 저장 실패", it) }
        }
    }

    fun observe(): Flow<MaintenanceSnapshot> {
        // 주행 중 주행거리가 자주 바뀌므로 0.1 km 단위로 줄여 바뀔 때만 다시 계산
        val odometer = signals.odometerKm
            .map { v -> v?.takeIf { it > 0f }?.let { floor(it * 10.0) / 10.0 } }
            .distinctUntilChanged()
        return combine(repository.observeRecords(), repository.observeBaseline(), odometer) { records, base, odo ->
            val d = today()
            MaintenanceSnapshot(MaintenanceCalculator.calculate(records, base, d, odo), records, odo, d)
        }.flowOn(Dispatchers.Default)
    }

    suspend fun add(entry: MaintenanceEntry): Long = repository.add(entry, clock())

    private companion object {
        const val TAG = "MaintenanceSource"
    }
}
