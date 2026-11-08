package com.tracers.trace.probe

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.ChargerOperator
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.tts.TextToSpeech
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import com.tracers.trace.BuildConfig
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.stt.listener.OnServerConnectionListener as SttConn
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit
import ai.pleos.playground.tts.listener.OnServerConnectionListener as TtsConn

/** C. NaviHelper / D. FusedLocation / E. STT·TTS */
class ServiceProbe(context: Context, private val board: ProbeBoard) {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var navi: NaviHelper? = null
    private var fused: FusedLocation? = null
    private var stt: SpeechToText? = null
    private var tts: TextToSpeech? = null
    private var lastLat = 0.0
    private var lastLon = 0.0
    private var geocoded = false
    private var recent: RecentDestinationInfo? = null
    private var testCount = 0

    private val clientId = BuildConfig.PLEOS_CLIENT_ID
    private val clientSecret = BuildConfig.PLEOS_CLIENT_SECRET
    private val hasKeys get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    fun start() {
        startNavi()
        startFused()
        startStt()
        startTts()
    }

    fun readAgain() {
        requestLocation()
        fused?.let { f -> runCatching { board.value(FUSED, f.isSupported()) }.onFailure { board.error(FUSED, it) } }
    }

    // ---------- C. NaviHelper ----------

    private val naviListener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
            board.value(LOC, fields(info))
            board.value(LOC_RAW, info.toString())
            lastLat = info.latitude
            lastLon = info.longitude
            if (!geocoded) { geocoded = true; geocode() }
        }

        // 기획 검증 1: 장소 이름이 들어 있는 반환 클래스들
        override fun onDestinationInfo(info: DestinationInfo) = board.value(P_DEST, fields(info))
        override fun onWayPointInfo(list: List<WaypointInfo>) =
            board.value(P_WAYPOINT, "${list.size}개: " + list.joinToString(" / ") { fields(it) })
        override fun onRecentDestinationInfo(list: List<RecentDestinationInfo>) {
            list.firstOrNull { !it.poiId.isNullOrEmpty() }?.let { recent = it }
            board.value(P_RECENT, "${list.size}개: " + list.take(5).joinToString(" / ") { "${it.name}|${it.address}|poi=${it.poiId}" })
        }
        override fun onBookmarkInfo(list: List<BookmarkInfo>) =
            board.value(P_BOOKMARK, "${list.size}개: " + list.take(5).joinToString(" / ") { "${it.name}|${it.category}|${it.address}" })
        override fun onRouteStarted(info: RouteStartInfo) = board.value(P_ROUTE_START, fields(info))
        override fun onDestinationArrived(info: DestinationArrivedInfo) = board.value(P_ARRIVED, fields(info))
        override fun onRoutingCompleted() = board.value(P_ROUTE_START, "onRoutingCompleted")
        override fun onRouteCancelled() = board.value(P_ROUTE_START, "onRouteCancelled")

        // 기획 검증 2: 과속방지턱(CautionPointType.SPEED_BUMP)·단속카메라
        override fun onCautionPointInfo(list: List<CautionPoint>) =
            board.value(P_CAUTION, "${list.size}개: " + list.joinToString { "${it.type}@${it.remainDistance}m" })
        override fun onCameraAlert(list: List<CameraAlert>) =
            board.value(P_CAMERA, "${list.size}개: " + list.joinToString { "${it.type}@${it.remainDistance}m 제한${it.speedLimit}" })

        override fun onCurrentRoadSpeedLimit(speedLimit: Int) = board.value(SPEED_LIMIT, speedLimit)

        override fun onNavigationStatus(status: NavigationStatus) = board.value(NAVI_STATUS, status)

        override fun onError(errorCode: NaviErrorCode) = board.error(LOC, "onError $errorCode")

        // EV: 충전 관련. DrivingState(목적지·경유지)에 충전 계획이 들어 있어 경로 안내 중일 때만 온다
        override fun onChargerOperatorInfo(list: List<ChargerOperator>) =
            board.value(EV_OPERATOR, "${list.size}개: " + list.joinToString { "${it.id}/${it.name}" })

        override fun onDrivingInfo(info: DrivingInfo) {
            board.value(EV_DRIVING, "destination={${info.destination?.let { fields(it) }}} " +
                    "chargingPlan={${info.destination?.chargingPlan?.let { fields(it) }}} waypoints=${info.waypoints?.size}")
        }

        override fun onRouteStateInfo(info: RouteStateInfo) = board.value(EV_ROUTE, fields(info))

        override fun onTripInfo(info: TripInfo) {
            board.value(EV_TRIP, fields(info))
            // 경로 위 지점(출발·경유·충전소·도착)의 poiName
            board.value(P_TRIP_POINTS, info.summary?.routePoints?.joinToString(" / ") {
                "${it.pointType}/${it.poiType}/${it.settingType} name=${it.poiName} poi=${it.poiId}"
            } ?: "routePoints=null")
        }
    }

    private fun startNavi() {
        listOf(NAVI, LOC, LOC_RAW, SPEED_LIMIT, NAVI_STATUS, EV_OPERATOR, EV_DRIVING, EV_ROUTE, EV_TRIP).forEach(board::declare)
        listOf(P_DEST, P_WAYPOINT, P_RECENT, P_BOOKMARK, P_ROUTE_START, P_ARRIVED, P_TRIP_POINTS, P_SEARCH,
            P_CAUTION, P_CAMERA, P_GEO_PRESENT, P_GEO).forEach(board::declare)
        board.support(P_SEARCH, "NaviHelper에 좌표·키워드 검색 메서드 없음. requestNavigationScreen(SearchPlace/NearbyChargingStations)은 내비 화면만 열고 결과를 돌려주지 않음")
        board.support(P_CAUTION, "onCautionPointInfo(List<CautionPoint>) type=CautionPointType.SPEED_BUMP 등, 경로·안전 안내 중에만 옴")
        board.support(EV_DRIVING, "경로 안내 중일 때 onDrivingInfo로 옴 (충전기 수·최대출력·목표SOC·충전계획)")
        board.support(EV_TRIP, "requestTrip(TripRequest) 응답으로만 옴 → 이번 PROBE에선 호출 안 함")
        // CurrentLocationInfo 필드(navi-data-3.0.4): latitude, longitude, heading, address, routeId, currentRouteIndex
        board.support(SPEED_LIMIT, "CurrentLocationInfo에는 없음. 별도 콜백 onCurrentRoadSpeedLimit(int)")
        try {
            val n = NaviHelper(appContext)
            navi = n
            n.addListener(naviListener)
            n.initialize({
                board.value(NAVI, "initialize 성공")
                requestLocation()
            }, { board.error(NAVI, it) })
        } catch (t: Throwable) {
            board.error(NAVI, t)
        }
    }

    private fun requestLocation() {
        val n = navi ?: return
        try {
            n.getCurrentLocationInfo()
            board.value(NAVI, "getCurrentLocationInfo 요청함")
            n.getChargerOperatorInfo()
            n.getRouteStateInfo()
            n.getDestinationInfo()
            n.getWaypointInfo()
            n.getRecentDestinationInfo()
            n.getBookmarkInfo()
        } catch (t: Throwable) {
            board.error(LOC, t)
        }
    }

    /** 기획 검증 1-b: 현재 위치 근처를 목적지로 넣고(이름 비움) 내비가 이름을 채워 주는지, 먼 곳 requestTrip의 충전소 이름 */
    fun requestTestRoute() {
        val n = navi ?: return board.error(P_ROUTE_START, "NaviHelper 없음")
        if (lastLat == 0.0) return board.error(P_ROUTE_START, "현재 위치 아직 없음")
        // 짝수 번째: 좌표만(이름 비움) / 홀수 번째: 최근 목적지의 poiId·좌표만 넣고 이름·주소는 비움
        val r = recent.takeIf { testCount++ % 2 == 1 }
        try {
            n.requestRoute(RouteInfo(latitude = r?.latitude ?: (lastLat + 0.01), longitude = r?.longitude ?: (lastLon + 0.01),
                displayLatitude = null, displayLongitude = null, poiName = "", poiId = r?.poiId ?: "", address = "",
                poiSubId = r?.poiSubId ?: "", routeOption = RouteOption.RECOMMENDED, poiProvider = r?.poiProvider ?: "",
                isMobileRequest = false))
            board.value(P_ROUTE_START, if (r == null) "requestRoute(lat+0.01, lon+0.01, poiName 비움) 보냄"
                else "requestRoute(최근 목적지 poiId=${r.poiId} provider=${r.poiProvider}, poiName 비움) 보냄")
        } catch (t: Throwable) {
            board.error(P_ROUTE_START, t)
        }
        try {
            // 부산역 좌표(공개 좌표): 충전 경유지가 붙는지 보려고 멀리
            n.requestTrip(TripRequest(35.1151, 129.0415, "", "", ""))
            board.value(EV_TRIP, "requestTrip(부산역 좌표) 보냄")
        } catch (t: Throwable) {
            board.error(EV_TRIP, t)
        }
        main.postDelayed({ requestLocation() }, 8_000)
    }

    fun cancelTestRoute() {
        runCatching { navi?.cancelRoute(); board.value(P_ROUTE_START, "cancelRoute 보냄") }.onFailure { board.error(P_ROUTE_START, it) }
    }

    // ---------- 기획 검증 1-d. Geocoder ----------

    private fun geocode() {
        board.value(P_GEO_PRESENT, Geocoder.isPresent())
        try {
            Geocoder(appContext, Locale.KOREA).getFromLocation(lastLat, lastLon, 5, object : Geocoder.GeocodeListener {
                override fun onGeocode(list: MutableList<Address>) =
                    board.value(P_GEO, "${list.size}개 ($lastLat,$lastLon): " + list.joinToString(" / ") { address(it) })

                override fun onError(errorMessage: String?) = board.error(P_GEO, "onError $errorMessage")
            })
        } catch (t: Throwable) {
            board.error(P_GEO, t)
        }
    }

    private fun address(a: Address) = "feature=${a.featureName} premises=${a.premises} thoroughfare=${a.thoroughfare} " +
            "subLocality=${a.subLocality} locality=${a.locality} admin=${a.adminArea} line0=${a.getAddressLine(0)}"

    /** 반환 객체의 getter를 전부 출력 (필드 목록을 손으로 적지 않기 위해) */
    private fun fields(o: Any): String = o.javaClass.methods
        .filter { it.parameterCount == 0 && it.name.startsWith("get") && it.name != "getClass" && it.declaringClass == o.javaClass }
        .sortedBy { it.name }
        .joinToString { m ->
            val name = m.name.removePrefix("get").replaceFirstChar { it.lowercase() }
            "$name=${runCatching { ProbeBoard.fmt(m.invoke(o)) }.getOrElse { "오류 $it" }}"
        }

    // ---------- D. FusedLocation ----------

    private fun startFused() {
        board.declare(FUSED)
        try {
            val f = FusedLocation(appContext)
            fused = f
            f.initialize()
            board.value(FUSED, "initialize 직후 isSupported=${f.isSupported()}")
            // 바인딩이 비동기일 수 있어 한 번 더 확인
            main.postDelayed({
                runCatching { board.value(FUSED, "1.5초 후 isSupported=${f.isSupported()}") }
                    .onFailure { board.error(FUSED, it) }
            }, 1_500)
        } catch (t: Throwable) {
            board.error(FUSED, t)
        }
    }

    // ---------- E. STT·TTS ----------

    private fun startStt() {
        listOf(STT_INIT, STT_REG).forEach(board::declare)
        try {
            val s = SpeechToText(appContext, SttMode.SERVER)
            stt = s
            s.initialize(object : SttInit {
                override fun onSuccess() {
                    board.value(STT_INIT, "onSuccess")
                    if (!hasKeys) {
                        board.support(STT_REG, "키 없음 (local.properties PLEOS_CLIENT_ID/SECRET)"); return
                    }
                    runCatching {
                        s.registerApp(clientId, clientSecret, object : SttConn {
                            override fun onConnected() = board.value(STT_REG, "onConnected")
                            override fun onFailed(errorType: SttError) = board.error(STT_REG, "onFailed $errorType")
                        })
                    }.onFailure { board.error(STT_REG, it) }
                }

                override fun onFailure(errorType: SttError) = board.error(STT_INIT, "onFailure $errorType")
            })
        } catch (t: Throwable) {
            board.error(STT_INIT, t)
        }
        if (!hasKeys) board.support(STT_REG, "키 없음 (local.properties PLEOS_CLIENT_ID/SECRET)")
    }

    private fun startTts() {
        listOf(TTS_INIT, TTS_REG).forEach(board::declare)
        try {
            val t = TextToSpeech(appContext, TtsMode.SERVER)
            tts = t
            t.initialize(object : TtsInit {
                override fun onSuccess() {
                    board.value(TTS_INIT, "onSuccess")
                    if (!hasKeys) {
                        board.support(TTS_REG, "키 없음 (local.properties PLEOS_CLIENT_ID/SECRET)"); return
                    }
                    runCatching {
                        t.registerApp(clientId, clientSecret, object : TtsConn {
                            override fun onConnected() = board.value(TTS_REG, "onConnected")
                            override fun onFailed(errorType: TtsError) = board.error(TTS_REG, "onFailed $errorType")
                        })
                    }.onFailure { board.error(TTS_REG, it) }
                }

                override fun onFailure(errorType: TtsError) = board.error(TTS_INIT, "onFailure $errorType")
            })
        } catch (t: Throwable) {
            board.error(TTS_INIT, t)
        }
        if (!hasKeys) board.support(TTS_REG, "키 없음 (local.properties PLEOS_CLIENT_ID/SECRET)")
    }

    fun stop() {
        main.removeCallbacksAndMessages(null)
        runCatching { navi?.removeListener(naviListener) }
        runCatching { navi?.release() }
        runCatching { fused?.release() }
        runCatching { stt?.release() }
        runCatching { tts?.release() }
    }

    companion object {
        const val NAVI = "C·NaviHelper 연결"
        const val LOC = "C·getCurrentLocationInfo 필드"
        const val LOC_RAW = "C·CurrentLocationInfo.toString"
        const val SPEED_LIMIT = "C·제한속도 onCurrentRoadSpeedLimit"
        const val NAVI_STATUS = "C·onNavigationStatus"
        const val FUSED = "D·FusedLocation.isSupported"
        const val EV_OPERATOR = "EV·C·충전 사업자 getChargerOperatorInfo"
        const val EV_DRIVING = "EV·C·경로 충전정보 onDrivingInfo"
        const val EV_ROUTE = "EV·C·경로 상태 getRouteStateInfo"
        const val EV_TRIP = "EV·C·도착 예상 SOC onTripInfo"
        const val P_DEST = "기획1·목적지 getDestinationInfo"
        const val P_WAYPOINT = "기획1·경유지 getWaypointInfo"
        const val P_RECENT = "기획1·최근 목적지 getRecentDestinationInfo"
        const val P_BOOKMARK = "기획1·즐겨찾기 getBookmarkInfo"
        const val P_ROUTE_START = "기획1·경로 시작 onRouteStarted"
        const val P_ARRIVED = "기획1·도착 onDestinationArrived"
        const val P_TRIP_POINTS = "기획1·requestTrip 경로 지점 routePoints"
        const val P_SEARCH = "기획1·주변 장소 검색"
        const val P_GEO_PRESENT = "기획1·Geocoder.isPresent"
        const val P_GEO = "기획1·Geocoder.getFromLocation"
        const val P_CAUTION = "기획2·주의 지점 onCautionPointInfo"
        const val P_CAMERA = "기획2·단속카메라 onCameraAlert"
        const val STT_INIT = "E·STT initialize"
        const val STT_REG = "E·STT registerApp"
        const val TTS_INIT = "E·TTS initialize"
        const val TTS_REG = "E·TTS registerApp"
    }
}
