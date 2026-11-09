package com.tracers.trace.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 항목 주기를 어디서부터 셌는지 */
enum class MaintenanceBasis {
    /** 그 항목의 마지막 정비 기록 */
    LAST_RECORD,
    /** 기록이 없어 앱이 처음 본 날짜·주행거리 ("기준: 처음 기록일") */
    FIRST_SEEN,
}

/** 사용률 단계 (D-01: 경고 ≥ 90%, 주의 ≥ 75%) */
enum class MaintenanceLevel { NORMAL, CAUTION, WARN }

/** 거리·기간 중 어느 쪽이 먼저 오는지 */
enum class MaintenanceMeasure { KM, TIME }

/** 거리 기준 사용량. [remainingKm]은 지나면 음수 */
data class KmUsage(val usedKm: Double, val intervalKm: Int) {
    val remainingKm: Double get() = intervalKm - usedKm
    val ratio: Double get() = usedKm / intervalKm
    /** 다음 점검 총 주행거리 */
    fun dueOdometerKm(basisOdometerKm: Double): Double = basisOdometerKm + intervalKm
}

/** 기간 기준 사용량. [remainingDays]는 지나면 음수 */
data class TimeUsage(
    val elapsedDays: Long,
    val totalDays: Long,
    val dueDate: LocalDate,
    val remainingDays: Long,
    val intervalMonths: Int,
) {
    val ratio: Double get() = if (totalDays <= 0) 1.0 else elapsedDays.toDouble() / totalDays
    /** 지난 개월 (소수, 화면 "11.6 / 12개월") */
    val elapsedMonths: Double get() = elapsedDays / DAYS_PER_MONTH

    private companion object {
        const val DAYS_PER_MONTH = 365.25 / 12
    }
}

/**
 * 점검 항목 하나의 상태.
 *
 * @param km 거리 기준. 거리 주기가 없거나 지금 주행거리를 모르면 null
 * @param time 기간 기준. 기간 주기가 없으면 null
 */
data class MaintenanceItemStatus(
    val type: MaintenanceItemType,
    val basis: MaintenanceBasis,
    val basisDate: LocalDate,
    val basisOdometerKm: Double,
    val km: KmUsage?,
    val time: TimeUsage?,
) {
    /** 먼저 오는 쪽(사용률이 큰 쪽). 둘 다 모르면 null */
    val leading: MaintenanceMeasure? get() = when {
        km == null && time == null -> null
        km == null -> MaintenanceMeasure.TIME
        time == null -> MaintenanceMeasure.KM
        km.ratio >= time.ratio -> MaintenanceMeasure.KM
        else -> MaintenanceMeasure.TIME
    }

    /** 먼저 오는 쪽 기준 사용률. 1.0 = 100%, 지나면 1 초과. 모르면 0 */
    val usageRatio: Double get() = when (leading) {
        MaintenanceMeasure.KM -> km!!.ratio
        MaintenanceMeasure.TIME -> time!!.ratio
        null -> 0.0
    }

    val level: MaintenanceLevel get() = when {
        usageRatio >= MaintenanceThresholds.WARN_RATIO -> MaintenanceLevel.WARN
        usageRatio >= MaintenanceThresholds.CAUTION_RATIO -> MaintenanceLevel.CAUTION
        else -> MaintenanceLevel.NORMAL
    }

    /** 점검 임박 (D-02): 남은 기간 14일 이하 또는 남은 거리 500 km 이하 (지난 것 포함) */
    val isImminent: Boolean get() =
        (time != null && time.remainingDays <= MaintenanceThresholds.IMMINENT_DAYS) ||
            (km != null && km.remainingKm <= MaintenanceThresholds.IMMINENT_KM)
}

/**
 * 점검 주기 계산 (D-01). 순수 함수.
 *
 * - 항목마다 마지막 정비 기록 기준. 기록이 없으면 앱이 처음 본 날짜·주행거리 기준
 * - 거리·기간 중 먼저 오는 쪽(사용률이 큰 쪽) 기준 사용률
 * - 급한 순(사용률 큰 순, 같으면 항목 정의 순서). 사용률을 모르는 항목은 맨 뒤
 */
object MaintenanceCalculator {

    fun calculate(
        records: List<MaintenanceEntry>,
        baseline: MaintenanceBaseline?,
        today: LocalDate,
        odometerKm: Double?,
    ): List<MaintenanceItemStatus> {
        val statuses = MaintenanceItemType.entries.mapNotNull { type ->
            val last = records.filter { type in it.items }
                .maxWithOrNull(compareBy<MaintenanceEntry>({ it.date }, { it.odometerKm }, { it.id }))
            when {
                last != null -> statusOf(
                    type, type.intervalKm, type.intervalMonths, MaintenanceBasis.LAST_RECORD,
                    last.date, last.odometerKm.toDouble(), today, odometerKm,
                )
                baseline != null -> statusOf(
                    type, type.intervalKm, type.intervalMonths, MaintenanceBasis.FIRST_SEEN,
                    baseline.date, baseline.odometerKm, today, odometerKm,
                )
                else -> null
            }
        }
        return statuses.sortedWith(
            compareBy<MaintenanceItemStatus> { it.leading == null }
                .thenByDescending { it.usageRatio }
                .thenBy { it.type.ordinal }
        )
    }

    /** 가장 급한 점검 임박 항목. 없으면 null. [statuses]는 [calculate] 결과(급한 순) */
    fun mostImminent(statuses: List<MaintenanceItemStatus>): MaintenanceItemStatus? =
        statuses.firstOrNull { it.isImminent }

    internal fun statusOf(
        type: MaintenanceItemType,
        intervalKm: Int?,
        intervalMonths: Int?,
        basis: MaintenanceBasis,
        basisDate: LocalDate,
        basisOdometerKm: Double,
        today: LocalDate,
        odometerKm: Double?,
    ): MaintenanceItemStatus {
        val km = if (intervalKm != null && odometerKm != null) {
            KmUsage(usedKm = (odometerKm - basisOdometerKm).coerceAtLeast(0.0), intervalKm = intervalKm)
        } else null
        val time = intervalMonths?.let { months ->
            val due = basisDate.plusMonths(months.toLong())
            TimeUsage(
                elapsedDays = ChronoUnit.DAYS.between(basisDate, today).coerceAtLeast(0),
                totalDays = ChronoUnit.DAYS.between(basisDate, due),
                dueDate = due,
                remainingDays = ChronoUnit.DAYS.between(today, due),
                intervalMonths = months,
            )
        }
        return MaintenanceItemStatus(type, basis, basisDate, basisOdometerKm, km, time)
    }
}
