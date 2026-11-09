package com.tracers.trace.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracers.trace.data.record.EfficiencySnapshot
import com.tracers.trace.domain.CoinWallet
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.EfficiencySummary
import com.tracers.trace.domain.FlightStatus
import com.tracers.trace.domain.FlightStatusResolver
import com.tracers.trace.domain.KmRecord
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * B-03: 주행 중 무대 문구 (거칠었음 > 매끄럽게·천천히)
 * B-05: 주행 모드에서는 큰 숫자 + 상태 1줄만 표시
 */
class HomeViewModel(
    /** A-01 주행 상태. 앱 전체에서 DriveRecordCollector 하나가 판정한 값을 받는다 */
    val driveState: StateFlow<DriveState>,
    /** C-01·C-02 전비·충전 예측 (Room 기록 기반). 기록이 없으면 summary.averageKmPerKwh == null */
    efficiency: Flow<EfficiencySnapshot>,
    /** B-03 충전 중 */
    charging: StateFlow<Boolean>,
    /** 런타임 권한이 모두 허용됐는지. false면 무대에 "차량 정보 권한이 필요해요" */
    permissionsGranted: StateFlow<Boolean>,
    /** E-01·E-02 안전점수·코인 지갑 */
    coinWallet: Flow<CoinWallet>,
    /** 가장 최근 주행의 1km 기록 (이번 주행 적립) */
    latestTripKms: Flow<List<KmRecord>>,
    /** B-04 거칠었음 */
    roughDriving: StateFlow<Boolean>,
    /** 차속 (m/s). B-03 천천히·매끄럽게 */
    speedMps: StateFlow<Float?>,
) : ViewModel() {

    private val flightResolver = FlightStatusResolver()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 차속은 100ms마다 바뀌므로 문구로 먼저 줄여서 바뀔 때만 넘긴다
        val flight = combine(driveState, speedMps, roughDriving) { st, v, rough ->
            flightResolver.resolve(st, (v ?: 0f) * 3.6, rough)
        }.distinctUntilChanged()
        val coin = combine(
            coinWallet.onStart { emit(CoinWallet()) },
            latestTripKms.onStart { emit(emptyList()) },
        ) { w, kms -> w to kms }
        val eff = combine(efficiency.onStart<EfficiencySnapshot?> { emit(null) }, charging) { e, c -> e to c }

        viewModelScope.launch {
            combine(
                driveState,
                eff,
                permissionsGranted,
                flight,
                coin,
            ) { st, (e, chg), granted, fl, (w, kms) -> Inputs(st, e, chg, granted, fl, w, kms) }
                .collect(::updateDriveMode)
        }
    }

    private data class Inputs(
        val state: DriveState,
        val snapshot: EfficiencySnapshot?,
        val charging: Boolean,
        val granted: Boolean,
        val flight: FlightStatus?,
        val wallet: CoinWallet,
        val latestTripKms: List<KmRecord>,
    )

    private fun updateDriveMode(input: Inputs) {
        val driveState = input.state
        val efficiency = input.snapshot?.summary
        var iconPath: String? = null
        val permissionsGranted = input.granted
        val isDriving = driveState == DriveState.DRIVING
        val caption: String
        val captionSub: String

        // TODO(B-03): 충전·점검 임박 상태는 해당 기능이 생기면 앞에 붙인다
        if (!permissionsGranted) {
            caption = "차량 정보 권한이 필요해요"
            captionSub = ""
        } else if (isDriving) {
            when (input.flight) {
                FlightStatus.ROUGH -> {
                    caption = "조금 거칠었어요"
                    captionSub = "급가속·급감속이 잦았어요"
                }
                FlightStatus.SLOW -> {
                    caption = "천천히 비행 중"
                    captionSub = "1 km마다 별이 생겨요"
                }
                else -> {
                    caption = "매끄럽게 비행 중"
                    captionSub = "1 km마다 별이 생겨요"
                }
            }
        } else {
            // B-03 정차 쪽: 충전 중 > 점검 임박(TODO D-02) > 충전 타이밍 > 쉬는 중
            val c = ParkedCaptionMapper.map(input.charging, input.snapshot)
            caption = c.title
            captionSub = c.sub
            if (c.title != "쉬는 중") iconPath = BOLT_ICON_PATH
        }

        _uiState.value = _uiState.value.copy(
            isDriving = isDriving,
            caption = caption,
            captionSub = captionSub,
            captionIconPath = iconPath,
            efficiency = EfficiencyTileMapper.map(efficiency, isDriving),
            maintenance = TileState.Maintenance(
                value = "–",
                unit = "일",
                status = "데이터를 모으는 중이에요",
                statusLevel = StatusLevel.Normal,
                barPercent = 0f
            ),
            coin = CoinTileMapper.map(input.wallet, input.latestTripKms, driveState),
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

    private companion object {
        /** 충전 문구 앞 번개 아이콘 (디자인 HomeDark 정차 문구) */
        const val BOLT_ICON_PATH = "M13 3L5 14h6l-1 7 8-11h-6z"
    }
}
