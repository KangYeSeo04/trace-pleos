package com.tracers.trace.ui.screens.home

import com.tracers.trace.domain.KmRecord
import com.tracers.trace.domain.CoinWallet
import com.tracers.trace.domain.DriveState
import com.tracers.trace.domain.DrivingThresholds
import java.util.Locale

/**
 * 지갑·최근 주행 1km 기록 → 홈 코인 타일. 순수 함수.
 *
 * - 값: 보유 코인
 * - 상태: 주행 중 "별 모으는 중", 주차 후 "이번 주행 +N" (E-02 적립은 주차 후에만 보임),
 *   80점 미만이면 "적립 멈춤"(주의색 + 글자)
 * - 막대: 안전점수 [BAR_MIN]~100을 0~100%로, 적립 기준 80이 눈금(50%)
 */
object CoinTileMapper {
    private const val BAR_MIN = 60.0
    private const val BAR_MAX = 100.0

    fun map(wallet: CoinWallet, latestTripKms: List<KmRecord>, state: DriveState): TileState.Coin {
        val score = wallet.safetyScore
        val accruing = score >= DrivingThresholds.COIN_ACCRUAL_MIN_SCORE
        val earned = latestTripKms.sumOf { it.coinsEarned }
        val (status, level) = when {
            state == DriveState.DRIVING -> (if (accruing) "별 모으는 중" else "적립 멈춤") to
                (if (accruing) StatusLevel.Gold else StatusLevel.Caution)
            state == DriveState.PARKED && earned > 0 -> "이번 주행 +$earned" to StatusLevel.Gold
            !accruing -> "적립 멈춤 · 80점부터 다시" to StatusLevel.Caution
            else -> "1 km마다 별이 생겨요" to StatusLevel.Gold
        }
        return TileState.Coin(
            value = String.format(Locale.KOREA, "%,d", wallet.balance),
            unit = "",
            status = status,
            statusLevel = level,
            barPercent = barPercent(score),
            barMarkPercent = barPercent(DrivingThresholds.COIN_ACCRUAL_MIN_SCORE),
            subText = String.format(Locale.KOREA, "안전점수 %.1f · 적립 기준 %d", score, DrivingThresholds.COIN_ACCRUAL_MIN_SCORE.toInt()),
        )
    }

    private fun barPercent(score: Double): Float =
        ((score - BAR_MIN) / (BAR_MAX - BAR_MIN) * 100).coerceIn(0.0, 100.0).toFloat()
}
