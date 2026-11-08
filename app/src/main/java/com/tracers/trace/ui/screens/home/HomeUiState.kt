package com.tracers.trace.ui.screens.home

import androidx.compose.ui.graphics.Color

/**
 * 홈 화면(S01 정차, S02 주행) UI 상태.
 */
data class HomeUiState(
    /** 주행 모드 여부. true면 S02, false면 S01 */
    val isDriving: Boolean = false,

    /** 비행 무대 캡션 (주요) */
    val caption: String = "",
    /** 비행 무대 캡션 (보조) */
    val captionSub: String = "",
    /** 캡션 아이콘 path (null이면 아이콘 없음) */
    val captionIconPath: String? = null,

    /** 전비 타일 */
    val efficiency: TileState = TileState.Efficiency(),
    /** 정비 타일 */
    val maintenance: TileState = TileState.Maintenance(),
    /** 코인 타일 */
    val coin: TileState = TileState.Coin(),
    /** 지역 타일 */
    val region: TileState = TileState.Region()
)

/**
 * 타일 상태. 종류별로 sealed class로 구분.
 */
sealed class TileState {
    abstract val value: String
    abstract val unit: String
    abstract val status: String
    abstract val statusLevel: StatusLevel
    abstract val barPercent: Float
    abstract val barMarkPercent: Float? // null이면 눈금 없음
    abstract val subText: String

    /**
     * 전비 타일
     */
    data class Efficiency(
        override val value: String = "0.0",
        override val unit: String = "km/kWh",
        override val status: String = "",
        override val statusLevel: StatusLevel = StatusLevel.Normal,
        override val barPercent: Float = 0f,
        override val barMarkPercent: Float? = null,
        override val subText: String = ""
    ) : TileState()

    /**
     * 정비 타일
     */
    data class Maintenance(
        override val value: String = "0",
        override val unit: String = "일",
        override val status: String = "",
        override val statusLevel: StatusLevel = StatusLevel.Normal,
        override val barPercent: Float = 0f,
        override val barMarkPercent: Float? = null,
        override val subText: String = ""
    ) : TileState()

    /**
     * 코인 타일
     */
    data class Coin(
        override val value: String = "0",
        override val unit: String = "",
        override val status: String = "",
        override val statusLevel: StatusLevel = StatusLevel.Gold,
        override val barPercent: Float = 0f,
        override val barMarkPercent: Float? = null,
        override val subText: String = ""
    ) : TileState()

    /**
     * 지역 타일
     */
    data class Region(
        val city: String = "",
        val district: String = "",
        /** 현재 위치 좌표 (서울 지도 viewBox 100x80 기준) */
        val locationX: Float = 50f,
        val locationY: Float = 40f,
        override val value: String = "",
        override val unit: String = "",
        override val status: String = "",
        override val statusLevel: StatusLevel = StatusLevel.Normal,
        override val barPercent: Float = 0f,
        override val barMarkPercent: Float? = null,
        override val subText: String = ""
    ) : TileState()
}

/**
 * 상태 레벨 (색상 결정용)
 */
enum class StatusLevel {
    /** t2 색상 (기본) */
    Normal,
    /** good 색상 */
    Good,
    /** caution 색상 */
    Caution,
    /** warn 색상 */
    Warn,
    /** gold 색상 */
    Gold
}
