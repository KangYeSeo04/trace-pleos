package com.tracers.trace.ui.screens.efficiency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.data.record.EfficiencySource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 전비 상세 화면 (S03). Room 기록과 계산 결과를 [EfficiencyUiState]로 바꿔 내보낸다. */
class EfficiencyViewModel(source: EfficiencySource) : ViewModel() {

    val uiState: StateFlow<EfficiencyUiState> = source.observe()
        .map { EfficiencyUiMapper.map(it.summary, it.forecast, it.trips, it.charges, it.currentPercent, it.nowMs, it.zone) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EfficiencyUiState(isLoading = true))
}
