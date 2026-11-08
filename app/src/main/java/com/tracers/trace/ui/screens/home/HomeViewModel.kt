package com.tracers.trace.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.data.vehicle.Gear
import com.tracers.trace.data.vehicle.VehicleSignals
import com.tracers.trace.data.sample.SampleData
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.DriveStateDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 홈 화면 ViewModel.
 *
 * A-01: 주행 상태 판정 → 정차/주행 모드 전환
 * B-05: 주행 모드에서는 큰 숫자 + 상태 1줄만 표시
 */
class HomeViewModel(
    private val vehicleSignals: VehicleSignals
) : ViewModel() {

    private val driveStateDetector = DriveStateDetector(viewModelScope)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 현재 주행 상태 (A-02 자동 복귀에서 사용) */
    val driveState: StateFlow<DriveState> = driveStateDetector.currentState

    init {
        vehicleSignals.start()

        // 기어와 속도를 DriveStateDetector에 전달
        viewModelScope.launch {
            combine(
                vehicleSignals.gear,
                vehicleSignals.speedMps
            ) { gear, speedMps ->
                gear to speedMps
            }.collect { (gear, speedMps) ->
                driveStateDetector.update(gear, speedMps)
            }
        }

        // DriveState → HomeUiState.isDriving
        viewModelScope.launch {
            driveStateDetector.currentState.collect { driveState ->
                updateDriveMode(driveState)
            }
        }
    }

    private fun updateDriveMode(driveState: DriveState) {
        val isDriving = driveState == DriveState.DRIVING

        // TODO(feat): SDK 연결 후 실제 데이터로 교체
        // 현재는 샘플 데이터 사용 (디자인 확인용)
        val sample = SampleData.homeUiState

        _uiState.value = sample.copy(
            isDriving = isDriving,
            caption = if (isDriving) "비행 중" else sample.caption,
            captionSub = if (isDriving) "순항 고도에서 안정적으로 비행 중이에요" else sample.captionSub
        )
    }

    override fun onCleared() {
        super.onCleared()
        vehicleSignals.stop()
    }
}
