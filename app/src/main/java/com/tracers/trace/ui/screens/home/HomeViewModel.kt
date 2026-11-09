package com.tracers.trace.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.EfficiencySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 홈 화면 ViewModel.
 *
 * A-01: 주행 상태 판정 → 정차/주행 모드 전환
 * B-05: 주행 모드에서는 큰 숫자 + 상태 1줄만 표시
 */
class HomeViewModel(
    /** A-01 주행 상태. 앱 전체에서 DriveRecordCollector 하나가 판정한 값을 받는다 */
    val driveState: StateFlow<DriveState>,
    /** C-01 전비 계산 결과 (Room 기록 기반). 기록이 없으면 summary.averageKmPerKwh == null */
    efficiency: Flow<EfficiencySummary>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // DriveState → HomeUiState.isDriving
        viewModelScope.launch {
            combine(driveState, efficiency.onStart<EfficiencySummary?> { emit(null) }) { st, eff -> st to eff }
                .collect { (st, eff) -> updateDriveMode(st, eff) }
        }
    }

    private fun updateDriveMode(driveState: DriveState, efficiency: EfficiencySummary?) {
        val isDriving = driveState == DriveState.DRIVING
        val caption: String
        val captionSub: String

        // TODO(B-03): 기체 상태 6종에 따라 캡션 변경
        if (isDriving) {
            caption = "비행 중"
            captionSub = "데이터를 모으는 중이에요"
        } else {
            // TODO(B-03, C-02): 점검 임박, 충전 타이밍 등 우선순위에 따라 캡션 표시
            caption = "정차 중"
            captionSub = ""
        }

        _uiState.value = _uiState.value.copy(
            isDriving = isDriving,
            caption = caption,
            captionSub = captionSub,
            efficiency = EfficiencyTileMapper.map(efficiency, isDriving),
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

}
