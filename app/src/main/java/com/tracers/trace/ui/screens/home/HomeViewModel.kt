package com.tracers.trace.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.data.vehicle.Gear
import com.tracers.trace.data.vehicle.VehicleSignals
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
        val caption: String
        val captionSub: String

        // 주행 모드 캡션 (B-03 연결 전까지는 기본 문구)
        // TODO(B-03): 기체 상태 6종에 따라 캡션 변경
        if (isDriving) {
            caption = "비행 중"
            captionSub = "데이터를 모으는 중이에요"
        } else {
            // 정차 모드 캡션 (점검·충전 상태는 B-03, C-02 연결 후)
            // TODO(B-03, C-02): 점검 임박, 충전 타이밍 등 우선순위에 따라 캡션 표시
            caption = "정차 중"
            captionSub = ""
        }

        _uiState.value = _uiState.value.copy(
            isDriving = isDriving,
            caption = caption,
            captionSub = captionSub,
            // 타일 데이터는 아직 연결 전 → 빈 상태 유지
            efficiency = TileState.Efficiency(
                value = "–",
                unit = "km/kWh",
                status = "데이터를 모으는 중이에요",
                statusLevel = StatusLevel.Normal,
                barPercent = 0f
            ),
            maintenance = TileState.Maintenance(
                value = "–",
                unit = "일",
                status = "데이터를 모으는 중이에요",
                statusLevel = StatusLevel.Normal,
                barPercent = 0f
            ),
            coin = TileState.Coin(
                value = "0",
                unit = "",
                status = if (isDriving) "별 모으는 중" else "주행 시작 시 적립",
                statusLevel = StatusLevel.Gold,
                barPercent = 0f,
                barMarkPercent = 0.5f
            ),
            region = TileState.Region(
                city = "–",
                district = "–",
                value = "–",
                unit = "",
                status = "데이터를 모으는 중이에요",
                statusLevel = StatusLevel.Normal,
                barPercent = 0f
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        vehicleSignals.stop()
    }
}
