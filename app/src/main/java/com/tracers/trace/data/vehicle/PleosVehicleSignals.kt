package com.tracers.trace.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.EvBatteryLevelListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VehicleSignals의 Pleos SDK + AAOS 구현체.
 *
 * 속도는 AAOS CarPropertyManager, 나머지는 Vehicle SDK를 사용.
 * SignalLogger.kt와 probe/VehicleProbe.kt에서 검증된 코드를 옮겨옴.
 */
class PleosVehicleSignals(context: Context) : VehicleSignals {

    private val appContext = context.applicationContext
    private val vehicle = Vehicle(appContext)
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val main = Handler(Looper.getMainLooper())
    private var started = false

    // StateFlow 백업 필드
    private val _speedMps = MutableStateFlow<Float?>(null)
    private val _gear = MutableStateFlow<Gear?>(null)
    private val _brakePedal = MutableStateFlow<Boolean?>(null)
    private val _steeringAngle = MutableStateFlow<Float?>(null)
    private val _turnSignal = MutableStateFlow<TurnSignal?>(null)
    private val _batteryPercent = MutableStateFlow<Int?>(null)
    private val _batteryLevelWh = MutableStateFlow<Float?>(null)
    private val _rangeRemainingM = MutableStateFlow<Float?>(null)
    private val _chargePortOpen = MutableStateFlow<Boolean?>(null)
    private val _odometerKm = MutableStateFlow<Float?>(null)

    override val speedMps: StateFlow<Float?> = _speedMps.asStateFlow()
    override val gear: StateFlow<Gear?> = _gear.asStateFlow()
    override val brakePedal: StateFlow<Boolean?> = _brakePedal.asStateFlow()
    override val steeringAngle: StateFlow<Float?> = _steeringAngle.asStateFlow()
    override val turnSignal: StateFlow<TurnSignal?> = _turnSignal.asStateFlow()
    override val batteryPercent: StateFlow<Int?> = _batteryPercent.asStateFlow()
    override val batteryLevelWh: StateFlow<Float?> = _batteryLevelWh.asStateFlow()
    override val rangeRemainingM: StateFlow<Float?> = _rangeRemainingM.asStateFlow()
    override val chargePortOpen: StateFlow<Boolean?> = _chargePortOpen.asStateFlow()
    override val odometerKm: StateFlow<Float?> = _odometerKm.asStateFlow()

    // ---------- AAOS 속도 콜백 ----------

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = value.value as? Float ?: return
            _speedMps.value = mps
            log("속도", "%.2f m/s (%.1f km/h)".format(mps, mps * 3.6f))
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w(TAG, "속도 onErrorEvent propId=$propId zone=$zone")
        }
    }

    // ---------- Vehicle SDK 리스너 ----------

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) {
            _brakePedal.value = status
            log("브레이크", status)
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "브레이크 실패", e) }
    }

    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) {
            // 부호 보정: SDK 값의 부호가 콘솔과 반대 (콘솔 +90 → SDK -90)
            // 양수 = 오른쪽으로 통일하기 위해 부호 반전
            val corrected = angle?.let { -it }
            _steeringAngle.value = corrected
            log("조향각", corrected?.let { "%.1f°".format(it) })
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "조향각 실패", e) }
    }

    private val turnListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            val mapped = when (state) {
                TurnSignalValue.OFF -> TurnSignal.OFF
                TurnSignalValue.LEFT -> TurnSignal.LEFT
                TurnSignalValue.RIGHT -> TurnSignal.RIGHT
                // SDK에 HAZARD 값이 없음. 비상등은 LEFT+RIGHT 동시 점등으로 추정되나 확인 필요
                else -> null
            }
            _turnSignal.value = mapped
            log("방향지시등", mapped)
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "방향지시등 실패", e) }
    }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(vehicleGear: VehicleGear?) {
            val mapped = when (vehicleGear) {
                VehicleGear.GEAR_PARK -> Gear.PARK
                VehicleGear.GEAR_REVERSE -> Gear.REVERSE
                VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
                VehicleGear.GEAR_DRIVE -> Gear.DRIVE
                else -> Gear.UNKNOWN
            }
            _gear.value = mapped
            log("기어", mapped)
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "기어 실패", e) }
    }

    private val batteryPercentListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
            _batteryPercent.value = value
            log("배터리%", value)
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "배터리% 실패", e) }
    }

    private val batteryLevelListener = object : EvBatteryLevelListener {
        override fun onEvBatteryLevelUpdated(value: Float?) {
            _batteryLevelWh.value = value
            log("배터리Wh", value)
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "배터리Wh 실패", e) }
    }

    private val rangeListener = object : RangeRemainingListener {
        override fun onRangeRemainingChanged(value: Float?) {
            _rangeRemainingM.value = value
            log("주행가능거리", value?.let { "%.0fm".format(it) })
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "주행가능거리 실패", e) }
    }

    private val chargePortListener = object : EvChargePortOpenListener {
        override fun onEvChargePortOpenUpdated(open: Boolean?) {
            _chargePortOpen.value = open
            log("충전구", open)
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "충전구 실패", e) }
    }

    private val odometerListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(distance: Float?) {
            _odometerKm.value = distance
            log("주행거리", distance?.let { "%.3fkm".format(it) })
        }

        override fun onFailed(e: Exception) { Log.w(TAG, "주행거리 실패", e) }
    }

    // ---------- 시작/중지 ----------

    override fun start() {
        if (started) return
        started = true

        // Vehicle SDK 초기화
        runCatching { vehicle.initialize() }.onFailure {
            Log.e(TAG, "Vehicle 초기화 실패", it)
            return
        }

        // 차량 서비스에 연결될 시간을 준 뒤 구독 (probe 코드에서 검증된 방식)
        main.postDelayed({ subscribeVehicleSdk() }, 1_000)

        // AAOS 속도 구독
        subscribeAaosSpeed()
    }

    private fun subscribeVehicleSdk() {
        if (!started) return

        // 브레이크
        runCatching {
            vehicle.getBrake().getCurrentBrakePedalStatus({ _brakePedal.value = it }, {})
            vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        }.onFailure { Log.w(TAG, "브레이크 구독 실패", it) }

        // 조향각
        runCatching {
            vehicle.getSteeringWheel().getCurrentSteeringWheelAngle(
                { _steeringAngle.value = it?.let { angle -> -angle } },
                {}
            )
            vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener)
        }.onFailure { Log.w(TAG, "조향각 구독 실패", it) }

        // 방향지시등
        runCatching {
            vehicle.getTurnSignal().getCurrentTurnSignalState({ state ->
                _turnSignal.value = when (state) {
                    TurnSignalValue.OFF -> TurnSignal.OFF
                    TurnSignalValue.LEFT -> TurnSignal.LEFT
                    TurnSignalValue.RIGHT -> TurnSignal.RIGHT
                    else -> null
                }
            }, {})
            vehicle.getTurnSignal().registerTurnSignalState(turnListener)
        }.onFailure { Log.w(TAG, "방향지시등 구독 실패", it) }

        // 기어
        runCatching {
            vehicle.getGear().getCurrentGearSelectionState({ g ->
                _gear.value = when (g) {
                    VehicleGear.GEAR_PARK -> Gear.PARK
                    VehicleGear.GEAR_REVERSE -> Gear.REVERSE
                    VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
                    VehicleGear.GEAR_DRIVE -> Gear.DRIVE
                    else -> Gear.UNKNOWN
                }
            }, {})
            vehicle.getGear().registerGearSelection(gearListener)
        }.onFailure { Log.w(TAG, "기어 구독 실패", it) }

        // EV 배터리 %
        runCatching {
            vehicle.getEvBattery().getEvBatteryStateOfCharge({ _batteryPercent.value = it }, {})
            vehicle.getEvBattery().registerEvBatteryStateOfCharge(batteryPercentListener)
        }.onFailure { Log.w(TAG, "배터리% 구독 실패", it) }

        // EV 배터리 잔량
        runCatching {
            vehicle.getEvBattery().getEvBatteryLevel({ _batteryLevelWh.value = it }, {})
            vehicle.getEvBattery().registerEvBatteryLevel(batteryLevelListener)
        }.onFailure { Log.w(TAG, "배터리Wh 구독 실패", it) }

        // 주행 가능 거리
        runCatching {
            vehicle.getOdometer().getRangeRemainingDistance({ _rangeRemainingM.value = it }, {})
            vehicle.getOdometer().registerRangeRemainingListener(rangeListener)
        }.onFailure { Log.w(TAG, "주행가능거리 구독 실패", it) }

        // 충전구
        runCatching {
            vehicle.getEvBattery().isEvChargePortOpen({ _chargePortOpen.value = it }, {})
            vehicle.getEvBattery().registerEvChargePortOpen(chargePortListener)
        }.onFailure { Log.w(TAG, "충전구 구독 실패", it) }

        // 총 주행거리
        runCatching {
            vehicle.getOdometer().getCurrentDistanceDriven({ _odometerKm.value = it }, {})
            vehicle.getOdometer().registerDistanceDriven(odometerListener)
        }.onFailure { Log.w(TAG, "주행거리 구독 실패", it) }
    }

    private fun subscribeAaosSpeed() {
        runCatching {
            val c = Car.createCar(appContext)
            car = c
            val cpm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            carPropertyManager = cpm
            val ok = cpm.registerCallback(
                speedCallback,
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                CarPropertyManager.SENSOR_RATE_FASTEST
            )
            if (!ok) Log.w(TAG, "속도 registerCallback=false")
        }.onFailure { Log.w(TAG, "속도 구독 실패", it) }
    }

    override fun stop() {
        if (!started) return
        started = false
        main.removeCallbacksAndMessages(null)

        // Vehicle SDK 해제
        runCatching { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        runCatching { vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener) }
        runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(turnListener) }
        runCatching { vehicle.getGear().unregisterGearSelection(gearListener) }
        runCatching { vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(batteryPercentListener) }
        runCatching { vehicle.getEvBattery().unregisterEvBatteryLevel(batteryLevelListener) }
        runCatching { vehicle.getOdometer().unregisterRangeRemainingListener(rangeListener) }
        runCatching { vehicle.getEvBattery().unregisterEvChargePortOpen(chargePortListener) }
        runCatching { vehicle.getOdometer().unregisterDistanceDriven(odometerListener) }
        runCatching { vehicle.release() }

        // AAOS 해제
        runCatching { carPropertyManager?.unregisterCallback(speedCallback) }
        runCatching { car?.disconnect() }
    }

    private fun log(name: String, value: Any?) {
        Log.d(TAG, "${System.currentTimeMillis()} · $name · $value")
    }

    companion object {
        private const val TAG = "VehicleSignals"
    }
}
