package com.tracers.trace.ui.screens.region

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 지역 상세 화면 상태. 지금은 화면 동작(선택·스위치·지우기 확인)만 한다.
 * TODO(G-01): 현재 지역 판정, TODO(G-04): 방문 기록 저장·지우기·위치 기록 설정 저장을 연결한다.
 */
class RegionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegionUiState())
    val uiState: StateFlow<RegionUiState> = _uiState.asStateFlow()

    fun onSelectRegion(code: String) = _uiState.update { it.copy(selectedRegionCode = code) }

    fun onToggleLocationRecording(on: Boolean) = _uiState.update { it.copy(locationRecordingOn = on) }

    fun onClearAll() = _uiState.update { it.copy(visited = emptyList(), selectedRegionCode = null) }
}
