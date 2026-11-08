package com.tracers.trace.domain

import com.tracers.trace.data.vehicle.Gear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 주행 상태 판정 로직 (A-01).
 *
 * 입력:
 * - 기어 (PARK / REVERSE / NEUTRAL / DRIVE / UNKNOWN)
 * - 속도 (m/s)
 *
 * 출력:
 * - PARKED: P단
 * - STOPPED: D·R·N단, 속도 0
 * - DRIVING: D·R단, 속도 > 0
 *
 * 규칙:
 * - 같은 결과가 2초 동안 유지될 때만 상태를 바꾼다.
 * - 2초 미만 깜빡임은 무시된다.
 *
 * @param delayMs 상태 전환 지연 시간 (기본 2000ms). 테스트에서는 0으로 설정 가능.
 */
class DriveStateDetector(
    private val scope: CoroutineScope,
    private val delayMs: Long = 2000
) {
    private val _currentState = MutableStateFlow(DriveState.PARKED)
    val currentState: StateFlow<DriveState> = _currentState

    private var pendingState: DriveState? = null
    private var pendingJob: Job? = null

    /**
     * 기어와 속도 신호를 받아 상태를 갱신한다.
     *
     * @param gear 현재 기어 (null이면 이전 값 유지)
     * @param speedMps 현재 속도 m/s (null이면 이전 값 유지)
     */
    fun update(gear: Gear?, speedMps: Float?) {
        // null 신호는 무시 (이전 상태 유지)
        if (gear == null || speedMps == null) {
            return
        }

        val newState = determineState(gear, speedMps)

        // 현재 상태와 같으면 pendingJob 취소
        if (newState == _currentState.value) {
            pendingJob?.cancel()
            pendingJob = null
            pendingState = null
            return
        }

        // 새로운 상태가 이미 대기 중인 상태와 같으면 그대로 유지
        if (newState == pendingState) {
            return
        }

        // 새로운 상태가 다르면 이전 대기 작업 취소하고 타이머 시작
        pendingJob?.cancel()
        pendingState = newState
        pendingJob = scope.launch {
            delay(delayMs)
            _currentState.value = newState
            pendingState = null
        }
    }

    /**
     * 기어와 속도로 주행 상태를 판정한다.
     */
    private fun determineState(gear: Gear, speedMps: Float): DriveState {
        return when (gear) {
            Gear.PARK -> DriveState.PARKED
            Gear.DRIVE, Gear.REVERSE -> {
                if (speedMps > 0f) DriveState.DRIVING else DriveState.STOPPED
            }
            Gear.NEUTRAL -> DriveState.STOPPED
            Gear.UNKNOWN -> {
                // 기어 신호가 불명확하면 속도로만 판정
                if (speedMps > 0f) DriveState.DRIVING else DriveState.PARKED
            }
        }
    }
}

/**
 * 주행 상태 (A-01).
 */
enum class DriveState {
    /** P단 */
    PARKED,
    /** D·R·N단, 속도 0 */
    STOPPED,
    /** D·R단, 속도 > 0 */
    DRIVING
}
