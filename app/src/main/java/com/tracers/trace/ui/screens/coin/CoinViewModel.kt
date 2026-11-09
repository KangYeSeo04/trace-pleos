package com.tracers.trace.ui.screens.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.data.record.CoinRepository
import com.tracers.trace.domain.DriveState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 코인 상세 화면 (S06). 안전점수(E-01)·적립(E-02)·이번 주행 기록(E-03)·받기와 기록(E-04).
 * 이 화면의 back stack entry에 묶여, 화면을 나가면 구독도 멈춘다.
 */
class CoinViewModel(
    private val coins: CoinRepository,
    driveState: StateFlow<DriveState>,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<CoinUiState> = combine(
        coins.observeWallet(),
        coins.observeLatestTripKms(),
        coins.observeHistory(),
        driveState,
    ) { wallet, kms, history, st ->
        CoinUiMapper.map(wallet, kms, history, parked = st == DriveState.PARKED)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoinUiState())

    /** 받을 코인을 보유 코인에 더한다. 지갑이 바뀌면 버튼은 저절로 사라진다 */
    fun onClaimCoins() {
        viewModelScope.launch { coins.claim(clock()) }
    }
}
