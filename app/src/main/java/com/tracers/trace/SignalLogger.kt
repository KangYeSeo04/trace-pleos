package com.tracers.trace

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
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
import androidx.compose.runtime.mutableStateMapOf

/**
 * 6단계 신호 로거.
 * 브레이크·조향각·방향지시등·기어(Vehicle SDK)와 속도(AAOS)를 받아서
 * 1) Logcat에 "수신시각 · 신호 · 값 (+직전 수신 후 ms)"로 찍고
 * 2) 화면에 최신 값을 보여준다.
 *
 * Logcat 검색창에 "SIGNAL"을 입력하면 이 로그만 보인다.
 */
class SignalLogger(context: Context) {

    private val appContext = context.applicationContext
    private val vehicle = Vehicle(appContext)
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private val main = Handler(Looper.getMainLooper())
    private var started = false

    /** 화면 표시용: 신호 이름 → "값  (+간격ms)" */
    val latest = mutableStateMapOf<String, String>()
    private val lastAt = mutableMapOf<String, Long>()

    private fun record(name: String, value: Any?) {
        val now = System.currentTimeMillis()
        val gap = synchronized(lastAt) {
            val g = lastAt[name]?.let { now - it }
            lastAt[name] = now
            g
        }
        val text = "$value" + (gap?.let { "  (+${it}ms)" } ?: "")
        Log.d(TAG, "$now · $name · $text")
        main.post { latest[name] = text }   // 화면 갱신은 메인 스레드에서
    }

    private fun fail(name: String, e: Throwable) {
        Log.w(TAG, "$name 실패: ${e.message}", e)
        main.post { latest[name] = "오류: ${e.message}" }
    }

    // ---------- Vehicle SDK 리스너 ----------

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) = record(BRAKE, status)
        override fun onFailed(e: Exception) = fail(BRAKE, e)
    }

    private val steeringListener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) = record(STEERING, angle)
        override fun onFailed(e: Exception) = fail(STEERING, e)
    }

    private val turnListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = record(TURN, state)
        override fun onFailed(e: Exception) = fail(TURN, e)
    }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = record(GEAR, gear)
        override fun onFailed(e: Exception) = fail(GEAR, e)
    }

    // ---------- AAOS 속도 (Vehicle SDK에는 속도 API가 없음) ----------

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = value.value as? Float ?: return
            record(SPEED, "%.2f m/s (%.1f km/h)".format(mps, mps * 3.6f))
        }
        override fun onErrorEvent(propId: Int, zone: Int) =
            fail(SPEED, IllegalStateException("onErrorEvent propId=$propId zone=$zone"))
    }

    fun start() {
        if (started) return
        started = true

        runCatching { vehicle.initialize() }.onFailure { fail("Vehicle 초기화", it); return }

        // 차량 서비스에 연결될 시간을 잠깐 준 뒤 구독 (다른 팀 코드에서도 쓰는 방식)
        main.postDelayed({ subscribeVehicle() }, 1_000)
        subscribeSpeed()
    }

    private fun subscribeVehicle() {
        if (!started) return
        runCatching {
            vehicle.getBrake().getCurrentBrakePedalStatus({ record(BRAKE, it) }, { fail(BRAKE, it) })
            vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        }.onFailure { fail(BRAKE, it) }

        runCatching {
            vehicle.getSteeringWheel().getCurrentSteeringWheelAngle({ record(STEERING, it) }, { fail(STEERING, it) })
            vehicle.getSteeringWheel().registerSteeringWheelAngle(steeringListener)
        }.onFailure { fail(STEERING, it) }

        runCatching {
            vehicle.getTurnSignal().getCurrentTurnSignalState({ record(TURN, it) }, { fail(TURN, it) })
            vehicle.getTurnSignal().registerTurnSignalState(turnListener)
        }.onFailure { fail(TURN, it) }

        runCatching {
            vehicle.getGear().getCurrentGearSelectionState({ record(GEAR, it) }, { fail(GEAR, it) })
            vehicle.getGear().registerGearSelection(gearListener)
        }.onFailure { fail(GEAR, it) }
    }

    private fun subscribeSpeed() {
        runCatching {
            val c = Car.createCar(appContext)
            car = c
            val cpm = c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            carPropertyManager = cpm
            // 실제 갱신 주기를 재려고 FASTEST로 받는다. (실제 앱에서는 성능 기준에 맞게 낮출 것)
            val ok = cpm.registerCallback(
                speedCallback,
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                CarPropertyManager.SENSOR_RATE_FASTEST
            )
            if (!ok) fail(SPEED, IllegalStateException("registerCallback=false"))
        }.onFailure { fail(SPEED, it) }
    }

    fun stop() {
        if (!started) return
        started = false
        main.removeCallbacksAndMessages(null)
        runCatching { vehicle.getBrake().unregisterBrakePedalStatus(brakeListener) }
        runCatching { vehicle.getSteeringWheel().unregisterSteeringWheelAngle(steeringListener) }
        runCatching { vehicle.getTurnSignal().unregisterTurnSignalState(turnListener) }
        runCatching { vehicle.getGear().unregisterGearSelection(gearListener) }
        runCatching { vehicle.release() }
        runCatching { carPropertyManager?.unregisterCallback(speedCallback) }
        runCatching { car?.disconnect() }
    }

    companion object {
        const val TAG = "SIGNAL"
        const val BRAKE = "브레이크"
        const val STEERING = "조향각"
        const val TURN = "방향지시등"
        const val GEAR = "기어"
        const val SPEED = "속도"
        val ALL = listOf(BRAKE, STEERING, TURN, GEAR, SPEED)
    }
}