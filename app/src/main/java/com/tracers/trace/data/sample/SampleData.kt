package com.tracers.trace.data.sample

import com.tracers.trace.ui.screens.efficiency.*
import com.tracers.trace.ui.screens.home.*

/**
 * 앱 전체에서 사용하는 샘플 데이터.
 * 디자인 확인용. SDK 연결 완료 시 삭제.
 */
object SampleData {

    // ===== 홈 화면 =====

    val homeUiState = HomeUiState(
        isDriving = false,
        caption = "비행 중",
        captionSub = "순항 고도에서 안정적으로 비행 중이에요",
        efficiency = TileState.Efficiency(
            value = "5.8",
            unit = "km/kWh",
            status = "평균 이상이에요",
            statusLevel = StatusLevel.Good,
            barPercent = 72f,
            barMarkPercent = 58f,
            subText = "90일 평균 5.3 km/kWh"
        ),
        maintenance = TileState.Maintenance(
            value = "324",
            unit = "일",
            status = "점검까지 여유로워요",
            statusLevel = StatusLevel.Good,
            barPercent = 32f,
            barMarkPercent = null,
            subText = "엔진오일 · 2027.09.28"
        ),
        coin = TileState.Coin(
            value = "1,284",
            unit = "",
            status = "별 모으는 중",
            statusLevel = StatusLevel.Gold,
            barPercent = 64f,
            barMarkPercent = 50f,
            subText = "다음 등급까지 216"
        ),
        region = TileState.Region(
            city = "서울",
            district = "강남구",
            locationX = 72f,
            locationY = 52f,
            value = "서울",
            unit = "",
            status = "25개 구 중 18번째",
            statusLevel = StatusLevel.Normal,
            barPercent = 72f,
            barMarkPercent = null,
            subText = "방문 72% · 387 km"
        )
    )

    // ===== 전비 상세 화면 =====

    private val calendarDays = (1..30).map { day ->
        CalendarDay(
            dayOfMonth = day,
            hasDriving = day <= 6,
            hasCharging = day == 2 || day == 5,
            isFuture = day > 6,
            isToday = day == 6
        )
    }

    private val monthBars = listOf(
        MonthBar("5월", 6.2f),
        MonthBar("6월", 6.4f),
        MonthBar("7월", 5.1f),
        MonthBar("8월", 5.0f),
        MonthBar("9월", 6.1f),
        MonthBar("10월", 5.6f)
    )

    val efficiencyUiState = EfficiencyUiState(
        average90Days = EfficiencyAverage(
            avgEfficiency = 5.8f,
            todayComparison = TodayComparison(
                todayValue = 6.4f,
                difference = 1.1f,
                isAboveAverage = true
            ),
            summary = EfficiencySummary(
                regenBraking = 38f,
                hvac = 17,
                hardAccel = 4,
                charges = 11
            )
        ),
        chargeTiming = ChargeTiming(
            title = "목요일까지 충분해요",
            description = "배터리 64% · 하루 평균 34 km 기준",
            forecast = listOf(
                ChargeForecastDay("지금", 64, isToday = true),
                ChargeForecastDay("내일", 53, isToday = false),
                ChargeForecastDay("수", 42, isToday = false),
                ChargeForecastDay("목", 31, isToday = false),
                ChargeForecastDay("금", 19, isToday = false)
            )
        ),
        calendar = DrivingCalendar(
            year = 2026,
            month = 11,
            days = calendarDays,
            selectedDay = 2,
            selectedDayDetail = DayDetail(
                dateLabel = "11월 2일 월요일",
                durationMinutes = 58,
                distanceKm = 42.6f,
                efficiency = 6.4f,
                efficiencyDiff = 1.1f,
                isAboveAverage = true,
                chargingRecord = ChargingRecord(
                    timeRange = "08:10 – 08:52",
                    isFastCharging = true,
                    stationName = "[충전소 이름]",
                    startPercent = 31,
                    endPercent = 64,
                    addedKwh = 28f
                )
            )
        ),
        monthlyEfficiency = MonthlyEfficiency(
            months = monthBars,
            avg90Days = 5.8f,
            selectedMonth = null,
            dailyChart = null
        ),
        prediction = MonthPrediction(
            monthLabel = "11월",
            normalRangeKm = 380,
            predictedRangeKm = 323,
            changePercent = -15,
            tip = "히터 사용이 늘어 주행거리가 줄어요. 출발 10분 전 예약 공조를 켜 두면 덜 줄어들어요."
        )
    )
}
