package com.tracers.trace.probe

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.Area
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.control.CruiseControl
import ai.pleos.playground.vehicle.constant.control.DrivingModeType
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.CruiseControlStateListener
import ai.pleos.playground.vehicle.listener.CruiseControlTargetSpeedListener
import ai.pleos.playground.vehicle.listener.DrivingModeStateListener
import ai.pleos.playground.vehicle.listener.EvBatteryEfficiencyListener
import ai.pleos.playground.vehicle.listener.EvBatteryLevelListener
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import ai.pleos.playground.vehicle.listener.RangeRemainingListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatBeltNotBuckledWarningListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import ai.pleos.playground.vehicle.listener.FuelEconomyListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacAcStateListener
import ai.pleos.playground.vehicle.listener.LowWasherFluidWaringListener
import ai.pleos.playground.vehicle.listener.OilLevelStateListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.reflect.Method

/**
 * B. Pleos Vehicle SDK 2.3.2.
 * 메서드 이름은 Gradle 캐시의 Vehicle-2.3.2.aar를 javap로 열어 확인한 것만 쓴다.
 */
class VehicleProbe(context: Context, private val board: ProbeBoard) {

    private val vehicle = Vehicle(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private val cleanups = mutableListOf<() -> Unit>()

    fun start() {
        try {
            vehicle.initialize()
        } catch (t: Throwable) {
            board.error("B·Vehicle.initialize", t); return
        }
        board.value("B·Vehicle.initialize", "호출됨 (1초 후 조회)")
        // 차량 서비스 바인딩 시간을 잠깐 준다 (SignalLogger와 같은 방식)
        main.postDelayed({
            sweepCapabilities()
            readAll(register = true)
        }, 1_000)
    }

    fun readAgain() = readAll(register = false)

    // ---------- 모든 도메인의 checkXXXCapabilities ----------

    /**
     * Vehicle의 getXxx() 도메인마다 check*Capabilities 메서드를 SDK 클래스에서 직접 찾아 호출한다.
     * 같은 이름의 오버로드가 있으면 정보가 많은 쪽(Function2/3: config + 영역/범위)을 고른다.
     */
    private fun sweepCapabilities() {
        val domains = Vehicle::class.java.methods.filter {
            it.name.startsWith("get") && it.parameterCount == 0 && it.returnType.isInterface &&
                    it.returnType.name.startsWith("ai.pleos.playground.vehicle.api.")
        }.sortedBy { it.name }
        for (getter in domains) {
            val domain = getter.returnType.simpleName
            val api = try {
                getter.invoke(vehicle)
            } catch (t: Throwable) {
                board.error("B·cap·$domain", t); continue
            }
            getter.returnType.methods
                .filter { it.name.startsWith("check") && it.name.endsWith("Capabilities") && it.parameterCount == 2 }
                .groupBy { it.name }
                .map { (_, overloads) -> overloads.maxBy { arity(it) } }
                .sortedBy { it.name }
                .forEach { callCheck(domain, api, it) }
        }
    }

    private fun arity(m: Method) = when (m.parameterTypes[0]) {
        Function3::class.java -> 3
        Function2::class.java -> 2
        else -> 1
    }

    private fun callCheck(domain: String, api: Any, m: Method) {
        val item = "B·cap·$domain.${m.name.removePrefix("check").removeSuffix("Capabilities")}"
        board.declare(item)
        val onFail: (Exception) -> Unit = { board.error(item, it) }
        val onOk: Any = when (arity(m)) {
            3 -> { a: Any?, b: Any?, c: Any? -> board.support(item, "${cap(a)} | ${cap(b)} | ${cap(c)}") }
            2 -> { a: Any?, b: Any? -> board.support(item, "${cap(a)} | ${cap(b)}") }
            else -> { a: Any? -> board.support(item, cap(a)) }
        }
        try {
            m.invoke(api, onOk, onFail)
        } catch (t: Throwable) {
            board.error(item, t)
        }
    }

    private fun cap(v: Any?): String = when (v) {
        is CapabilityConfig -> "access=${v.accessType}"
        // Area 자체엔 getter가 없고 SeatArea·WindowArea 등 하위 클래스마다 getValues()가 있다
        is Area -> runCatching { (v.javaClass.getMethod("getValues").invoke(v) as Array<*>).joinToString("+") }
            .getOrDefault("$v").let { "${v.javaClass.simpleName}($it)" }
        is AreaBounds<*> -> "${v.minValue}~${v.maxValue}"
        is Collection<*> -> v.joinToString(prefix = "[", postfix = "]") { cap(it) }
        is Map<*, *> -> v.entries.joinToString(prefix = "{", postfix = "}") { "${cap(it.key)}=${cap(it.value)}" }
        is Array<*> -> v.joinToString(prefix = "[", postfix = "]") { cap(it) }
        else -> ProbeBoard.fmt(v)
    }

    // ---------- 값 읽기 + 변경 콜백 ----------

    private fun <T> read(item: String, call: (ok: (T?) -> Unit, fail: (Exception) -> Unit) -> Unit) {
        board.declare(item)
        try {
            call({ board.value(item, it) }, { board.error(item, it) })
        } catch (t: Throwable) {
            board.error(item, t)
        }
    }

    private fun listen(item: String, register: () -> Unit, unregister: () -> Unit) {
        try {
            register()
            cleanups += { runCatching(unregister) }
        } catch (t: Throwable) {
            board.error(item, t)
        }
    }

    private fun readAll(register: Boolean) {
        fuel(register)
        hvac(register)
        odometer(register)
        safety(register)
        wiper(register)
        brake(register)
        steering(register)
        turnSignal(register)
        gear(register)
        evBattery(register)
        evRange(register)
        evTire()
        evDrivingMode(register)
        evClimate()
        seat(register)
    }

    private fun fuel(register: Boolean) {
        val fuel = vehicle.getFuel()

        val pct = "B·Fuel.연료량(%) getFuelPercent"
        read<Int>(pct) { ok, fail -> fuel.getFuelPercent(ok, fail) }
        board.support("B·Fuel.탱크 용량", "SDK에 API 없음 → A·INFO_FUEL_CAPACITY로 확인")

        // 이름 → 내부 속성(FuelImpl 바이트코드에서 확인): 순간=INSTANTANEOUS_FUEL_ECONOMY,
        // 이번 주행 평균=TRIP_FUEL_ECONOMY(CURRENT), 주유 후 평균=SINCE_REFUEL_FUEL_ECONOMY, 누적 평균=CUMULATIVE_FUEL_ECONOMY
        val inst = "B·Fuel.순간 연비 getInstantaneousFuelEconomy"
        val cur = "B·Fuel.이번 주행 평균 연비 getCurrentFuelEconomy"
        val since = "B·Fuel.주유 후 평균 연비 getSinceChargeFuelEconomy"
        val cum = "B·Fuel.누적 평균 연비 getCumulativeFuelEconomy"
        read<Float>(inst) { ok, fail -> fuel.getInstantaneousFuelEconomy(ok, fail) }
        read<Float>(cur) { ok, fail -> fuel.getCurrentFuelEconomy(ok, fail) }
        read<Float>(since) { ok, fail -> fuel.getSinceChargeFuelEconomy(ok, fail) }
        read<Float>(cum) { ok, fail -> fuel.getCumulativeFuelEconomy(ok, fail) }
        if (!register) return

        val pctL = object : FuelPercentListener {
            override fun onFuelPercentUpdated(percent: Int?) = board.value(pct, percent)
            override fun onFailed(e: Exception) = board.error(pct, e)
        }
        listen(pct, { fuel.registerFuelPercent(pctL) }, { fuel.unregisterFuelPercent(pctL) })
        fun econ(item: String) = object : FuelEconomyListener {
            override fun onFuelEconomyUpdated(value: Float?) = board.value(item, value)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        econ(inst).let { l -> listen(inst, { fuel.registerInstantaneousFuelEconomy(l) }, { fuel.unregisterInstantaneousFuelEconomy(l) }) }
        econ(cur).let { l -> listen(cur, { fuel.registerCurrentFuelEconomy(l) }, { fuel.unregisterCurrentFuelEconomy(l) }) }
        econ(since).let { l -> listen(since, { fuel.registerSinceChargeFuelEconomy(l) }, { fuel.unregisterSinceChargeFuelEconomy(l) }) }
        econ(cum).let { l -> listen(cum, { fuel.registerCumulativeFuelEconomy(l) }, { fuel.unregisterCumulativeFuelEconomy(l) }) }
    }

    private fun hvac(register: Boolean) {
        val hvac = vehicle.getHVAC()
        val item = "B·HVAC.에어컨 getHvacAcState"
        board.declare(item)
        // 좌석 영역은 추측하지 않고 capability가 돌려준 영역 중 첫 번째를 쓴다
        try {
            hvac.checkHvacAcCapabilities({ cfg, areas ->
                board.support(item, "${cap(cfg)} areas=${cap(areas)}")
                val area = areas?.firstOrNull() ?: return@checkHvacAcCapabilities
                read<Boolean>(item) { ok, fail -> hvac.getHvacAcState(area, ok, fail) }
                if (register) {
                    val l = object : HvacAcStateListener {
                        override fun onHvacAcStateUpdated(state: Boolean?) = board.value(item, state)
                        override fun onFailed(e: Exception) = board.error(item, e)
                    }
                    listen(item, { hvac.registerHvacAcState(area, l) }, { hvac.unregisterHvacAcState(area, l) })
                }
            }, { board.error(item, it) })
        } catch (t: Throwable) {
            board.error(item, t)
        }
    }

    private fun odometer(register: Boolean) {
        val odo = vehicle.getOdometer()
        val total = "B·Odometer.총 주행거리 getCurrentDistanceDriven"
        val tripDist = "B·Odometer.이번 주행거리 getTripCurrentDrivingDistance"
        val tripTime = "B·Odometer.이번 주행시간 getTripCurrentDrivingTime"
        read<Float>(total) { ok, fail -> odo.getCurrentDistanceDriven(ok, fail) }
        read<Float>(tripDist) { ok, fail -> odo.getTripCurrentDrivingDistance(ok, fail) }
        read<Int>(tripTime) { ok, fail -> odo.getTripCurrentDrivingTime(ok, fail) }
        if (!register) return
        fun dist(item: String) = object : DistanceDrivenListener {
            override fun onDistanceDrivenUpdated(distance: Float?) = board.value(item, distance)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        dist(total).let { l -> listen(total, { odo.registerDistanceDriven(l) }, { odo.unregisterDistanceDriven(l) }) }
        dist(tripDist).let { l -> listen(tripDist, { odo.registerTripCurrentDrivingDistance(l) }, { odo.unregisterTripCurrentDrivingDistance(l) }) }
        val timeL = object : DrivingTimeListener {
            override fun onDrivingTimeUpdated(time: Int?) = board.value(tripTime, time)
            override fun onFailed(e: Exception) = board.error(tripTime, e)
        }
        listen(tripTime, { odo.registerTripCurrentDrivingTime(timeL) }, { odo.unregisterTripCurrentDrivingTime(timeL) })
    }

    private fun safety(register: Boolean) {
        val s = vehicle.getSafety()
        val oil = "B·Safety.엔진오일 getEngineOilLevelState"
        val brakeFluid = "B·Safety.브레이크액 getBrakeFluidLevelState"
        val tmOil = "B·Safety.변속기오일 getTransmissionOilLevelState"
        val washer = "B·Safety.워셔액 부족 hasLowWasherFluidWarning"
        val powerDown = "B·Safety.파워다운 경고 hasPowerDownWarningIndicator"
        val telltale = "B·Safety.경고등 getTelltalesInfo"
        val telltaleReg = "B·Safety.경고등 registerTelltales"
        read<OilLevelState>(oil) { ok, fail -> s.getEngineOilLevelState(ok, fail) }
        read<OilLevelState>(brakeFluid) { ok, fail -> s.getBrakeFluidLevelState(ok, fail) }
        read<OilLevelState>(tmOil) { ok, fail -> s.getTransmissionOilLevelState(ok, fail) }
        read<Boolean>(washer) { ok, fail -> s.hasLowWasherFluidWarning(ok, fail) }
        read<Boolean>(powerDown) { ok, fail -> s.hasPowerDownWarningIndicator(ok, fail) }
        board.declare(telltale)
        try {
            s.getTelltalesInfo({ board.value(telltale, telltales(it)) }, { board.error(telltale, it) })
        } catch (t: Throwable) {
            board.error(telltale, t)
        }
        if (!register) return
        fun oilL(item: String) = object : OilLevelStateListener {
            override fun onOilLevelUpdated(state: OilLevelState?) = board.value(item, state)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        oilL(oil).let { l -> listen(oil, { s.registerEngineOilLevelState(l) }, { s.unregisterEngineOilLevelState(l) }) }
        oilL(brakeFluid).let { l -> listen(brakeFluid, { s.registerBrakeFluidLevelState(l) }, { s.unregisterBrakeFluidLevelState(l) }) }
        oilL(tmOil).let { l -> listen(tmOil, { s.registerTransmissionOilLevelState(l) }, { s.unregisterTransmissionOilLevelState(l) }) }
        val washerL = object : LowWasherFluidWaringListener {
            override fun onLowWasherFluidWaringUpdated(warning: Boolean?) = board.value(washer, warning)
            override fun onFailed(e: Exception) = board.error(washer, e)
        }
        listen(washer, { s.registerLowWasherFluidWaringState(washerL) }, { s.unregisterLowWasherFluidWaringState(washerL) })
        // registerTelltales는 PRIVATE_CAR_SAFETY 필요 (선언하지 않음 → 권한 오류 확인용)
        board.declare(telltaleReg)
        val tl = object : TelltaleInfoListener {
            override fun onTelltaleInfoUpdated(info: Telltales?) = board.value(telltaleReg, telltales(info))
            override fun onFailed(e: Exception) = board.error(telltaleReg, e)
        }
        listen(telltaleReg, { s.registerTelltales(tl) }, { s.unregisterTelltales(tl) })
    }

    private fun telltales(t: Telltales?): String {
        val list = t?.telltales ?: return "null"
        return "${list.size}개: " + list.joinToString { "${it.type}=${it.state}(c${it.color},p${it.priority},w${it.warningCount})" }
    }

    private fun wiper(register: Boolean) {
        val w = vehicle.getWiper()
        val item = "B·Wiper.상태 getCurrentWiperState"
        board.declare(item)
        try {
            w.checkWiperStateCapabilities({ cfg, map ->
                board.support(item, "${cap(cfg)} ${cap(map)}")
                val area = map?.keys?.firstOrNull() ?: return@checkWiperStateCapabilities
                read<WiperState>(item) { ok, fail -> w.getCurrentWiperState(area, ok, fail) }
                if (register) {
                    val l = object : WiperStateListener {
                        override fun onWiperStateUpdated(state: WiperState?) = board.value(item, state)
                        override fun onFailed(e: Exception) = board.error(item, e)
                    }
                    listen(item, { w.registerWiperState(area, l) }, { w.unregisterWiperState(area, l) })
                }
            }, { board.error(item, it) })
        } catch (t: Throwable) {
            board.error(item, t)
        }
    }

    private fun brake(register: Boolean) {
        val b = vehicle.getBrake()
        // 시그니처가 Function1<Boolean> → on/off 값 (0~100 아님)
        val item = "B·Brake.페달 getCurrentBrakePedalStatus (Boolean)"
        read<Boolean>(item) { ok, fail -> b.getCurrentBrakePedalStatus(ok, fail) }
        if (!register) return
        val l = object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(status: Boolean?) = board.value(item, status)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        listen(item, { b.registerBrakePedalStatus(l) }, { b.unregisterBrakePedalStatus(l) })
    }

    private fun steering(register: Boolean) {
        val sw = vehicle.getSteeringWheel()
        val item = "B·SteeringWheel.각도 getCurrentSteeringWheelAngle"
        board.declare(item)
        try {
            sw.checkSteeringWheelAngleCapabilities({ cfg, bounds ->
                board.support(item, "${cap(cfg)} 범위=${cap(bounds)}")
            }, { board.error(item, it) })
        } catch (t: Throwable) {
            board.error(item, t)
        }
        read<Float>(item) { ok, fail -> sw.getCurrentSteeringWheelAngle(ok, fail) }
        if (!register) return
        val l = object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(angle: Float?) = board.value(item, angle)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        listen(item, { sw.registerSteeringWheelAngle(l) }, { sw.unregisterSteeringWheelAngle(l) })
    }

    private fun turnSignal(register: Boolean) {
        val ts = vehicle.getTurnSignal()
        val item = "B·TurnSignal.상태 getCurrentTurnSignalState"
        read<TurnSignalValue>(item) { ok, fail -> ts.getCurrentTurnSignalState(ok, fail) }
        if (!register) return
        val l = object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = board.value(item, state)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        listen(item, { ts.registerTurnSignalState(l) }, { ts.unregisterTurnSignalState(l) })
    }

    private fun gear(register: Boolean) {
        val g = vehicle.getGear()
        val item = "B·Gear.기어 getCurrentGearSelectionState"
        read<VehicleGear>(item) { ok, fail -> g.getCurrentGearSelectionState(ok, fail) }
        if (!register) return
        val l = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) = board.value(item, gear)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        listen(item, { g.registerGearSelection(l) }, { g.unregisterGearSelection(l) })
    }

    // ---------- EV 구역 ----------

    private fun evBattery(register: Boolean) {
        val b = vehicle.getEvBattery()
        // 이름 → 내부 속성(EvBatteryImpl 바이트코드에서 확인):
        // 순간=INSTANTANEOUS_EV_EFFICIENCY, 이번 주행 평균=TRIP_EV_EFFICIENCY(CURRENT),
        // 충전 후 평균=SINCE_CHARGE_EV_EFFICIENCY, 누적 평균=CUMULATIVE_EV_EFFICIENCY
        val inst = "EV·B·순간 전비 getInstantaneousEvEfficiency"
        val cur = "EV·B·이번 주행 평균 전비 getCurrentEvEfficiency"
        val since = "EV·B·충전 후 평균 전비 getSinceChargeEvEfficiency"
        val cum = "EV·B·누적 평균 전비 getCumulativeEvEfficiency"
        val soc = "EV·B·배터리 % getEvBatteryStateOfCharge (EV_BATTERY_SOC)"
        val level = "EV·B·배터리 잔량 getEvBatteryLevel (EV_BATTERY_LEVEL)"
        val cap = "EV·B·배터리 용량 getEvBatteryCapacity (INFO_EV_BATTERY_CAPACITY)"
        val port = "EV·B·충전구 열림 isEvChargePortOpen"
        val sw = "EV·B·충전 스위치 getEvChargeSwitchState"
        val limit = "EV·B·충전 전류 제한 getEvChargeCurrentDrawLimit"
        read<Float>(inst) { ok, fail -> b.getInstantaneousEvEfficiency(ok, fail) }
        read<Float>(cur) { ok, fail -> b.getCurrentEvEfficiency(ok, fail) }
        read<Float>(since) { ok, fail -> b.getSinceChargeEvEfficiency(ok, fail) }
        read<Float>(cum) { ok, fail -> b.getCumulativeEvEfficiency(ok, fail) }
        read<Int>(soc) { ok, fail -> b.getEvBatteryStateOfCharge(ok, fail) }
        read<Float>(level) { ok, fail -> b.getEvBatteryLevel(ok, fail) }
        read<Float>(cap) { ok, fail -> b.getEvBatteryCapacity(ok, fail) }
        read<Boolean>(port) { ok, fail -> b.isEvChargePortOpen(ok, fail) }
        read<Boolean>(sw) { ok, fail -> b.getEvChargeSwitchState(ok, fail) }
        read<Float>(limit) { ok, fail -> b.getEvChargeCurrentDrawLimit(ok, fail) }
        board.support("EV·B·충전 상태·속도·완료시간·배터리온도", "SDK에 API 없음 (EvChargeState enum만 있고 쓰는 메서드 없음) → EV·A 확인")
        if (!register) return
        fun eff(item: String) = object : EvBatteryEfficiencyListener {
            override fun onEvBatteryEfficiencyUpdated(value: Float?) = board.value(item, value)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        eff(inst).let { l -> listen(inst, { b.registerInstantaneousEvEfficiency(l) }, { b.unregisterInstantaneousEvEfficiency(l) }) }
        eff(cur).let { l -> listen(cur, { b.registerCurrentEvEfficiency(l) }, { b.unregisterCurrentEvEfficiency(l) }) }
        eff(since).let { l -> listen(since, { b.registerSinceChargeEvEfficiency(l) }, { b.unregisterSinceChargeEvEfficiency(l) }) }
        eff(cum).let { l -> listen(cum, { b.registerCumulativeEvEfficiency(l) }, { b.unregisterCumulativeEvEfficiency(l) }) }
        val socL = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(value: Int?) = board.value(soc, value)
            override fun onFailed(e: Exception) = board.error(soc, e)
        }
        listen(soc, { b.registerEvBatteryStateOfCharge(socL) }, { b.unregisterEvBatteryStateOfCharge(socL) })
        val levelL = object : EvBatteryLevelListener {
            override fun onEvBatteryLevelUpdated(value: Float?) = board.value(level, value)
            override fun onFailed(e: Exception) = board.error(level, e)
        }
        listen(level, { b.registerEvBatteryLevel(levelL) }, { b.unregisterEvBatteryLevel(levelL) })
        val portL = object : EvChargePortOpenListener {
            override fun onEvChargePortOpenUpdated(open: Boolean?) = board.value(port, open)
            override fun onFailed(e: Exception) = board.error(port, e)
        }
        listen(port, { b.registerEvChargePortOpen(portL) }, { b.unregisterEvChargePortOpen(portL) })
    }

    private fun evRange(register: Boolean) {
        val odo = vehicle.getOdometer()
        val item = "EV·B·남은 주행가능거리 Odometer.getRangeRemainingDistance"
        read<Float>(item) { ok, fail -> odo.getRangeRemainingDistance(ok, fail) }
        if (!register) return
        val l = object : RangeRemainingListener {
            override fun onRangeRemainingChanged(value: Float?) = board.value(item, value)
            override fun onFailed(e: Exception) = board.error(item, e)
        }
        listen(item, { odo.registerRangeRemainingListener(l) }, { odo.unregisterRangeRemainingListener(l) })
    }

    private fun evTire() {
        val t = vehicle.getTire()
        val low = "EV·B·Tire 저압 경고 getLowTirePressure (CRITICALLY_LOW_TIRE_PRESSURE)"
        val pressure = "EV·B·Tire 공기압 getTirePressure (TIRE_PRESSURE, PRIVATE_CAR_SAFETY)"
        listOf(low, pressure).forEach(board::declare)
        try {
            t.checkLowTirePressureCapabilities({ cfg, areas ->
                board.support(low, "${cap(cfg)} ${cap(areas)}")
                areas?.forEach { a ->
                    val item = "$low ${cap(a)}"
                    read<Float>(item) { ok, fail -> t.getLowTirePressure(a, ok, fail) }
                }
            }, { board.error(low, it) })
        } catch (e: Throwable) {
            board.error(low, e)
        }
        // 영역은 checkWheelAreaCapabilities(CAR_SAFETY) 결과의 첫 번째
        try {
            t.checkWheelAreaCapabilities({ areas ->
                val a = areas?.firstOrNull() ?: return@checkWheelAreaCapabilities
                read<Float>(pressure) { ok, fail -> t.getTirePressure(a, ok, fail) }
            }, { board.error(pressure, it) })
        } catch (e: Throwable) {
            board.error(pressure, e)
        }
    }

    private fun evDrivingMode(register: Boolean) {
        val d = vehicle.getDrivingMode()
        val mode = "EV·B·드라이브 모드 getCurrentDrivingModeState"
        val cc = "EV·B·크루즈 상태 getCruiseControlState"
        val ccSpeed = "EV·B·크루즈 목표속도 getCruiseControlTargetSpeed"
        read<DrivingModeType>(mode) { ok, fail -> d.getCurrentDrivingModeState(ok, fail) }
        read<CruiseControl>(cc) { ok, fail -> d.getCruiseControlState(ok, fail) }
        read<Float>(ccSpeed) { ok, fail -> d.getCruiseControlTargetSpeed(ok, fail) }
        board.support("EV·B·회생제동 단계·패들", "SDK에 API 없음 (EvRegenerativeBrakingState enum만 있음) → EV·A 확인")
        if (!register) return
        val modeL = object : DrivingModeStateListener {
            override fun onDrivingModeStateUpdated(state: DrivingModeType?) = board.value(mode, state)
            override fun onFailed(e: Exception) = board.error(mode, e)
        }
        listen(mode, { d.registerDrivingModeState(modeL) }, { d.unregisterDrivingModeState(modeL) })
        val ccL = object : CruiseControlStateListener {
            override fun onCruiseControlStateUpdated(state: CruiseControl?) = board.value(cc, state)
            override fun onFailed(e: Exception) = board.error(cc, e)
        }
        listen(cc, { d.registerCruiseControlState(ccL) }, { d.unregisterCruiseControlState(ccL) })
        val speedL = object : CruiseControlTargetSpeedListener {
            override fun onCruiseControlTargetSpeedUpdated(speed: Float?) = board.value(ccSpeed, speed)
            override fun onFailed(e: Exception) = board.error(ccSpeed, e)
        }
        listen(ccSpeed, { d.registerCruiseControlTargetSpeed(speedL) }, { d.unregisterCruiseControlTargetSpeed(speedL) })
    }

    /** 겨울철 주행거리 감소 설명용: 공조 전원·설정온도·실내온도·풍량·열선 */
    private fun evClimate() {
        val h = vehicle.getHVAC()
        val power = "EV·B·HVAC 전원 getHvacPowerState"
        val fan = "EV·B·HVAC 풍량 getHvacFanSpeed"
        val setT = "EV·B·HVAC 설정온도 getHvacTemperature(°C)"
        val curT = "EV·B·HVAC 현재온도 getHvacTemperatureCurrent(°C)"
        val wheel = "EV·B·핸들 열선 getHvacSteeringWheelHeatState"
        listOf(power, fan, setT, curT).forEach(board::declare)
        try {
            h.checkHvacPowerCapabilities({ _, areas ->
                val a = areas?.firstOrNull() ?: return@checkHvacPowerCapabilities
                read<Boolean>(power) { ok, fail -> h.getHvacPowerState(a, ok, fail) }
            }, { board.error(power, it) })
            h.checkHvacFanSpeedCapabilities({ _, map ->
                val a = map?.keys?.firstOrNull() ?: return@checkHvacFanSpeedCapabilities
                read<Int>(fan) { ok, fail -> h.getHvacFanSpeed(a, ok, fail) }
            }, { board.error(fan, it) })
            h.checkHvacTemperatureCapabilities({ _, map ->
                map?.keys?.forEach { a ->
                    read<Float>("$setT ${cap(a)}") { ok, fail -> h.getHvacTemperature(a, TemperatureUnit.CELSIUS, ok, fail) }
                }
            }, { board.error(setT, it) })
            h.checkHvacTemperatureCurrentCapabilities({ _, areas ->
                areas?.forEach { a ->
                    read<Float>("$curT ${cap(a)}") { ok, fail -> h.getHvacTemperatureCurrent(a, TemperatureUnit.CELSIUS, ok, fail) }
                }
            }, { board.error(curT, it) })
        } catch (e: Throwable) {
            board.error(power, e)
        }
        read<Int>(wheel) { ok, fail -> vehicle.getSteeringWheel().getHvacSteeringWheelHeatState(ok, fail) }
    }

    /** 기획 검증 3: 좌석 탑승·벨트 (Seat: CAR_SEATS, Safety: CAR_SAFETY) */
    private fun seat(register: Boolean) {
        val seat = vehicle.getSeat()
        val safety = vehicle.getSafety()
        val occ = "기획3·B·Seat 탑승 getCurrentSeatOccupancy"
        val belt = "기획3·B·Seat 벨트 getCurrentSeatBeltBuckledState"
        val warn = "기획3·B·Safety 벨트 미착용 경고 hasSeatBeltNotBuckledWarning"
        listOf(occ, belt, warn).forEach(board::declare)

        fun perArea(base: String, check: ((Any?, List<SeatArea>?) -> Unit, (Exception) -> Unit) -> Unit,
                    each: (String, SeatArea) -> Unit) {
            try {
                check({ cfg, areas ->
                    board.support(base, "${cap(cfg)} ${cap(areas)}")
                    areas?.forEach { each("$base ${cap(it)}", it) }
                }, { board.error(base, it) })
            } catch (e: Throwable) {
                board.error(base, e)
            }
        }

        perArea(occ, { ok, fail -> seat.checkSeatOccupancyCapabilities({ c, a -> ok(c, a) }, fail) }) { item, a ->
            read<Boolean>(item) { ok, fail -> seat.getCurrentSeatOccupancy(a, ok, fail) }
            if (register) {
                val l = object : SeatOccupancyListener {
                    override fun onSeatOccupancyUpdated(occupied: Boolean?) = board.value(item, occupied)
                    override fun onFailed(e: Exception) = board.error(item, e)
                }
                listen(item, { seat.registerSeatOccupancy(a, l) }, { seat.unregisterSeatOccupancy(a, l) })
            }
        }
        perArea(belt, { ok, fail -> seat.checkSeatBeltBuckledCapabilities({ c, a -> ok(c, a) }, fail) }) { item, a ->
            read<Boolean>(item) { ok, fail -> seat.getCurrentSeatBeltBuckledState(a, ok, fail) }
            if (register) {
                val l = object : SeatBeltBuckledStateListener {
                    override fun onSeatBeltBuckledStateUpdated(buckled: Boolean?) = board.value(item, buckled)
                    override fun onFailed(e: Exception) = board.error(item, e)
                }
                listen(item, { seat.registerSeatBeltBuckledState(a, l) }, { seat.unregisterSeatBeltBuckledState(a, l) })
            }
        }
        perArea(warn, { ok, fail -> safety.checkSeatNotBuckledWarningCapabilities({ c, a -> ok(c, a) }, fail) }) { item, a ->
            read<Boolean>(item) { ok, fail -> safety.hasSeatBeltNotBuckledWarning(a, ok, fail) }
            if (register) {
                val l = object : SeatBeltNotBuckledWarningListener {
                    override fun onSeatBeltNotBuckledWarningUpdated(warning: Boolean?) = board.value(item, warning)
                    override fun onFailed(e: Exception) = board.error(item, e)
                }
                listen(item, { safety.registerSeatBeltNotBuckledWarning(a, l) }, { safety.unregisterSeatBeltNotBuckledWarning(a, l) })
            }
        }
    }

    fun stop() {
        main.removeCallbacksAndMessages(null)
        cleanups.forEach { it() }
        cleanups.clear()
        runCatching { vehicle.release() }
    }
}
