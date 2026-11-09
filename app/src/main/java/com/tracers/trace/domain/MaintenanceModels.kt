package com.tracers.trace.domain

import java.time.LocalDate

/** 정비 기록 입력 방식. 자동 감지 = 방문 감지(D-03) 카드에서 시작한 기록 */
enum class MaintenanceInputMethod { AUTO_DETECTED, MANUAL }

/**
 * 정비 기록 한 건 (D-04·D-05).
 *
 * @param items 이 기록으로 주기가 새로 시작되는 점검 항목
 * @param other "기타" 칩을 골랐는지. 주기 계산에는 쓰지 않는다
 * @param costWon 비용. null = 입력 안 함
 */
data class MaintenanceEntry(
    val id: Long = 0,
    val date: LocalDate,
    val odometerKm: Int,
    val place: String,
    val items: Set<MaintenanceItemType>,
    val other: Boolean = false,
    val costWon: Int? = null,
    val memo: String = "",
    val method: MaintenanceInputMethod = MaintenanceInputMethod.MANUAL,
)

/**
 * 앱이 처음 본 날짜와 총 주행거리. 기록이 없는 항목은 여기서부터 센다 ("기준: 처음 기록일").
 */
data class MaintenanceBaseline(
    val date: LocalDate,
    val odometerKm: Double,
)
