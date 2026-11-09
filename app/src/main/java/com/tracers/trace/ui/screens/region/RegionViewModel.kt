package com.tracers.trace.ui.screens.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.domain.region.RegionLookupData
import com.tracers.trace.domain.region.RegionStatus
import com.tracers.trace.domain.region.RegionVisit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 지역 상세(S08)·지역별 지우기 화면 상태 (G-01·G-03·G-04).
 * 방문 기록은 Room, 지금 지역은 RegionRecorder, 위치 기록 켜짐은 설정에서 받는다.
 */
class RegionViewModel(
    visits: Flow<List<RegionVisit>>,
    status: StateFlow<RegionStatus>,
    data: StateFlow<RegionLookupData?>,
    recordingOn: StateFlow<Boolean>,
    private val setRecording: (Boolean) -> Unit,
    private val deleteRegion: suspend (String) -> Unit,
    private val clearAll: suspend () -> Unit,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RegionUiState> = combine(visits, status, data, recordingOn, selected) { v, st, d, on, sel ->
        RegionUiState(
            visited = RegionUiMapper.visited(v, d),
            currentRegionCode = RegionUiMapper.currentCode(st),
            selectedRegionCode = sel,
            locationRecordingOn = on,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RegionUiState(locationRecordingOn = recordingOn.value))

    fun onSelectRegion(code: String) {
        selected.value = code
    }

    fun onToggleLocationRecording(on: Boolean) = setRecording(on)

    fun onDeleteRegion(code: String) {
        if (selected.value == code) selected.value = null
        viewModelScope.launch { deleteRegion(code) }
    }

    fun onClearAll() {
        selected.value = null
        viewModelScope.launch { clearAll() }
    }
}
