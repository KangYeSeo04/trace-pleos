package com.tracers.trace.data.region

import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.domain.DriveState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * NaviHelper 현재 위치를 일정 간격으로 요청한다 (G-01).
 *
 * - 주행 중(신호 대기 포함) [DRIVING_INTERVAL_MS], 주차 중 [PARKED_INTERVAL_MS]
 * - 주행 상태가 바뀌면 바로 한 번 요청하고 새 간격으로 다시 센다
 * - [enabled]가 false(위치 기록 꺼짐)면 요청하지 않는다
 *
 * 요청 한 번은 IPC 하나라 5초 간격이면 CPU 부담이 거의 없다.
 */
class LocationPoller(
    private val signals: VehicleSignals,
    private val driveState: Flow<DriveState>,
    private val enabled: Flow<Boolean>,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (job != null) return
        job = scope.launch {
            combine(driveState.map(::intervalFor).distinctUntilChanged(), enabled) { iv, on -> iv to on }
                .distinctUntilChanged()
                .collectLatest { (interval, on) ->
                    if (!on) return@collectLatest
                    while (true) {
                        signals.requestLocation()
                        delay(interval)
                    }
                }
        }
    }

    companion object {
        const val DRIVING_INTERVAL_MS = 5_000L
        const val PARKED_INTERVAL_MS = 60_000L

        fun intervalFor(state: DriveState): Long =
            if (state == DriveState.PARKED) PARKED_INTERVAL_MS else DRIVING_INTERVAL_MS
    }
}
