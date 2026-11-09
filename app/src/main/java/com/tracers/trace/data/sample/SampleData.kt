package com.tracers.trace.data.sample

import com.tracers.trace.ui.screens.coin.*
import com.tracers.trace.ui.screens.efficiency.*
import com.tracers.trace.ui.screens.home.*
import com.tracers.trace.ui.screens.maintenance.*
import com.tracers.trace.ui.screens.region.*
import java.time.LocalDate
import java.time.YearMonth

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

    // ===== 코인 상세 화면 (S06) =====

    /** 디자인의 위험 km (CoinDark.dc.html riskKm) */
    private val coinRiskKm = listOf(4, 9, 14, 20, 30)

    /** 디자인 수식대로 37 km 점수: 출발 83.0, 1km마다 기존 × 0.98 + 이번 × 0.02 (위험 km 40, 나머지 100) */
    private val coinScores: List<Float> = buildList {
        var s = 83.0
        add(s.toFloat())
        for (k in 1..37) {
            s = s * 0.98 + (if (k in coinRiskKm) 40.0 else 100.0) * 0.02
            add(s.toFloat())
        }
    }

    val coinUiState = CoinUiState(
        balance = 1284,
        starsThisTrip = 37,
        claimableCoins = 37,
        safetyScore = SafetyScoreChart(scores = coinScores, riskKm = coinRiskKm),
        tripRecord = TripKmRecord(
            cleanCells = (1..37).map { it !in coinRiskKm },
            hardAccel = 2,
            hardBrake = 1,
            speeding = 1,
            hardStop = 1,
        ),
        shop = ShopTeaser(nextItemName = "은하수 비행운", coinsToNext = 216),
        history = listOf(
            CoinHistoryEntry("11월 5일 퇴근", "31 km · 안전점수 85점대", 31),
            CoinHistoryEntry("11월 5일 출근", "29 km · 안전점수 84점대", 29),
            CoinHistoryEntry("11월 4일", "별가루 비행운 구매", -500),
            CoinHistoryEntry("11월 4일 퇴근", "33 km · 안전점수 84점대", 33),
        ),
    )

    /** 받을 코인은 없지만 주행 기록은 있는 상태. 점수 80 미만(적립 멈춤) */
    val coinUiStateStopped = CoinUiState(
        balance = 1284,
        starsThisTrip = 3,
        claimableCoins = 0,
        safetyScore = SafetyScoreChart(
            scores = listOf(81.0f, 80.4f, 79.6f, 79.2f, 78.9f, 79.3f),
            riskKm = listOf(1, 2, 3),
        ),
    )

    /** 빈 상태: 주행 기록 없음, 받을 코인 없음 */
    val coinUiStateEmpty = CoinUiState()

    // ===== 정비 상세 화면 (MaintDark.dc.html) =====

    private val maintenanceItems = listOf(
        MaintenanceItem("와이퍼 블레이드", "12일 남음", listOf(UsageGauge(97f, "11.6 / 12개월"))),
        MaintenanceItem("에어컨 필터", "1,200 km 남음", listOf(UsageGauge(88f, "8,800 / 10,000 km"))),
        MaintenanceItem("타이어 위치 교환", "2,100 km 남음", listOf(UsageGauge(79f, "7,900 / 10,000 km"))),
        MaintenanceItem("브레이크액", "7개월 남음", listOf(UsageGauge(71f, "17 / 24개월"))),
        MaintenanceItem("12V 배터리", "약 2년 남음", listOf(UsageGauge(50f, "24 / 48개월"))),
        MaintenanceItem("브레이크 패드 (앞)", "26,200 km 남음", listOf(UsageGauge(60f, "38,800 / 65,000 km"))),
        MaintenanceItem("감속기 오일 점검", "21,200 km 남음", listOf(UsageGauge(65f, "38,800 / 60,000 km"))),
        MaintenanceItem("냉각수 (첫 교체)", "161,200 km 남음", listOf(UsageGauge(19f, "38,800 / 200,000 km"))),
    )

    private fun maintRecord(
        id: Long, date: String, km: Int, title: String, place: String, auto: Boolean, items: List<String>,
        cost: Int? = null,
    ) = MaintenanceRecord(
        id = id,
        date = LocalDate.parse(date),
        odometerKm = km,
        title = title,
        place = place,
        method = if (auto) RecordInputMethod.AutoDetected else RecordInputMethod.Manual,
        itemNames = items,
        costWon = cost,
    )

    /** 금액은 디자인이 [금액] 자리표시라 임의 값 */
    private val maintenanceRecords = listOf(
        maintRecord(1, "2026-05-14", 30_900, "타이어 위치 교환 · 브레이크액 점검", "[정비소 이름]", true, listOf("타이어 위치 교환", "브레이크액"), 45_000),
        maintRecord(2, "2026-01-08", 30_000, "에어컨 필터 교체", "[정비소 이름]", true, listOf("에어컨 필터"), 25_000),
        maintRecord(3, "2025-11-20", 24_600, "와이퍼 블레이드 교체", "셀프 교체", false, listOf("와이퍼 블레이드")),
        maintRecord(4, "2025-08-02", 20_900, "타이어 위치 교환", "[정비소 이름]", true, listOf("타이어 위치 교환")),
        maintRecord(5, "2025-06-11", 19_800, "브레이크액 교체", "[정비소 이름]", true, listOf("브레이크액")),
        maintRecord(6, "2025-03-17", 17_200, "12V 배터리 점검 · 브레이크 패드 점검", "[정비소 이름]", true, listOf("12V 배터리", "브레이크 패드 (앞)")),
        maintRecord(7, "2024-12-09", 14_800, "와이퍼 블레이드 교체", "셀프 교체", false, listOf("와이퍼 블레이드")),
        maintRecord(8, "2024-09-23", 12_100, "에어컨 필터 교체 · 감속기 오일 점검", "[정비소 이름]", true, listOf("에어컨 필터", "감속기 오일 점검")),
        maintRecord(9, "2024-06-05", 10_000, "타이어 위치 교환 · 냉각수 점검", "[정비소 이름]", true, listOf("타이어 위치 교환", "냉각수 (첫 교체)")),
        maintRecord(10, "2023-11-28", 4_300, "와이퍼 블레이드 교체", "[정비소 이름]", true, listOf("와이퍼 블레이드")),
    )

    val maintenanceUiState = MaintenanceUiState(
        visit = VisitDetection(whenLabel = "어제", placeName = "[정비소 이름]", stayLabel = "1시간 12분"),
        items = maintenanceItems,
        records = maintenanceRecords,
        calendar = MaintenanceCalendar(month = YearMonth.of(2026, 5), selectedDate = LocalDate.of(2026, 5, 14)),
        form = MaintenanceRecordForm(
            dateText = "2026. 11. 5",
            odometerDigits = "38800",
            place = "[정비소 이름]",
            selectedItems = setOf("타이어 위치 교환"),
        ),
    )

    /** 두 기준(거리·기간) 막대 2줄 확인용. 디자인엔 이 경우가 없어 값은 임의 */
    val maintenanceUiStateTwoGauges = MaintenanceUiState(
        items = listOf(
            MaintenanceItem(
                "에어컨 필터", "1,200 km 남음",
                listOf(UsageGauge(88f, "8,800 / 10,000 km"), UsageGauge(76f, "9.1 / 12개월")),
            ),
        ) + maintenanceItems.drop(2),
    )

    /** 빈 상태: 방문 감지 없음, 점검 항목·기록 없음 */
    val maintenanceUiStateEmpty = MaintenanceUiState()

    // ===== 지역 상세 화면 (RegionDark.dc.html) =====

    /** 디자인 VIS 12곳(색 순서 = TraceColors.regionPalette), 값은 디자인 DEX·OLD 예시. 지금 지역 = 경주시 */
    val regionUiState = RegionUiState(
        visited = listOf(
            VisitedRegion("11000", "서울", "서울특별시", 0, 9840.0, 600_000),
            VisitedRegion("28000", "인천", "인천광역시", 1, 86.0, 5 * 60),
            VisitedRegion("41280", "고양시", "경기도", 2, 210.0, 3 * 24 * 60),
            VisitedRegion("41450", "하남시", "경기도", 3, 95.0, 26 * 60),
            VisitedRegion("41110", "수원시", "경기도", 4, 312.0, 6 * 60),
            VisitedRegion("51110", "춘천시", "강원특별자치도", 5, 140.0, 32 * 60),
            VisitedRegion("51130", "원주시", "강원특별자치도", 6, 75.0, 9 * 60),
            VisitedRegion("51150", "강릉시", "강원특별자치도", 7, 214.0, 190),
            VisitedRegion("47130", "경주시", "경상북도", 8, 128.0, 3240),
            VisitedRegion("52110", "전주시", "전북특별자치도", 9, 158.0, 9 * 60),
            VisitedRegion("26000", "부산", "부산광역시", 10, 205.0, 74 * 60),
            VisitedRegion("50110", "제주시", "제주특별자치도", 11, 260.0, 4 * 24 * 60),
        ),
        currentRegionCode = "47130",
    )

    /** 빈 상태: 방문 0곳, 지금 지역 모름 */
    val regionUiStateEmpty = RegionUiState()
}
