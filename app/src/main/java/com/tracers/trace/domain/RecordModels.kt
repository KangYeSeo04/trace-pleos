package com.tracers.trace.domain

/**
 * 계산에 쓰는 차량 값 한 묶음. 원시 신호는 이 객체로 받아 계산한 뒤 바로 버린다.
 *
 * @property timeMs 측정 시각 (epoch ms)
 * @property speedMps 차속 (m/s)
 * @property batteryKwh 배터리 잔량 (kWh). [BatteryEnergy.kwh]로 만든다
 * @property batteryPercent 배터리 % (0~100)
 * @property odometerKm 총 주행거리 (km)
 * @property hvacAcOn 공조 AC 켜짐
 * @property chargePortOpen 충전구 열림
 */
data class VehicleSample(
    val timeMs: Long,
    val speedMps: Float? = null,
    val batteryKwh: Double? = null,
    val batteryPercent: Int? = null,
    val odometerKm: Double? = null,
    val hvacAcOn: Boolean? = null,
    val chargePortOpen: Boolean? = null,
)

/** 주행 1회 요약. data 계층의 TripSummaryEntity와 1:1 */
data class TripRecord(
    val startedAtMs: Long,
    val endedAtMs: Long,
    val distanceKm: Double,
    val energyUsedKwh: Double,
    val hardAccelCount: Int,
    val regenKwh: Double,
    val hvacOnSec: Long,
    val startBatteryPercent: Int?,
    val endBatteryPercent: Int?,
) {
    /** 실제로 쓴 에너지 = 소비 − 회생 */
    val netEnergyKwh: Double get() = energyUsedKwh - regenKwh
    val durationSec: Long get() = (endedAtMs - startedAtMs) / 1000
}

/** 충전 1회 기록. data 계층의 ChargeSessionEntity와 1:1 */
data class ChargeRecord(
    val startedAtMs: Long,
    val endedAtMs: Long,
    val startPercent: Int,
    val endPercent: Int,
    val addedKwh: Double,
)

object BatteryEnergy {
    /**
     * 잔량 kWh. 잔량(Wh)이 오면 그 값을, 없으면 용량 × % 로 대체 계산한다 (A-06).
     */
    fun kwh(levelWh: Float?, percent: Int?, capacityWh: Float?): Double? = when {
        levelWh != null && levelWh >= 0f -> levelWh / 1000.0
        percent != null && capacityWh != null && capacityWh > 0f -> capacityWh / 1000.0 * percent / 100.0
        else -> null
    }
}
