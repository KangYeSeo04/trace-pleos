package com.tracers.trace.ui.screens.coin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 코인 상세 화면 (S06).
 * SDK·저장 연결 전이라 빈 상태만 내보낸다. 안전점수(E-01)·적립(E-02)·받기(E-04)는 2단계에서 연결.
 */
class CoinViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CoinUiState())
    val uiState: StateFlow<CoinUiState> = _uiState.asStateFlow()

    fun onClaimCoins() {
        // TODO(E-04): 받을 코인을 보유 코인에 더하고 받은 기록에 남긴다
    }
}
