package com.tracers.trace.probe

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context

/** A. AAOS CarPropertyManager: config(지원·접근), 현재 값, 변경 콜백 */
class AaosProbe(context: Context, private val board: ProbeBoard) {

    private val appContext = context.applicationContext
    private var car: Car? = null
    private var cpm: CarPropertyManager? = null

    private val base = listOf(
        VehiclePropertyIds.PERF_VEHICLE_SPEED,
        VehiclePropertyIds.GEAR_SELECTION,
        VehiclePropertyIds.FUEL_LEVEL,
        VehiclePropertyIds.FUEL_LEVEL_LOW,
        VehiclePropertyIds.INFO_FUEL_CAPACITY,
        VehiclePropertyIds.INFO_FUEL_TYPE,
        VehiclePropertyIds.PERF_ODOMETER,
        VehiclePropertyIds.HVAC_AC_ON,
        VehiclePropertyIds.ENGINE_OIL_LEVEL,
        VehiclePropertyIds.WINDSHIELD_WIPERS_STATE,
    )

    /** EV 구역: 단위는 AOSP VehiclePropertyIds 문서 기준 */
    private val ev = mapOf(
        VehiclePropertyIds.EV_BATTERY_LEVEL to "Wh",
        VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY to "Wh",
        VehiclePropertyIds.EV_CURRENT_BATTERY_CAPACITY to "Wh",
        VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED to "",
        VehiclePropertyIds.EV_CHARGE_PORT_OPEN to "",
        VehiclePropertyIds.EV_CHARGE_STATE to "",
        VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE to "mW",
        VehiclePropertyIds.EV_CHARGE_TIME_REMAINING to "s",
        VehiclePropertyIds.RANGE_REMAINING to "m",
        VehiclePropertyIds.EV_REGENERATIVE_BRAKING_STATE to "",
        VehiclePropertyIds.EV_BRAKE_REGENERATION_LEVEL to "",
        VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE to "°C",
    )

    /** 기획 검증 3: 좌석별 areaId를 모두 읽는다 */
    private val seat = listOf(VehiclePropertyIds.SEAT_OCCUPANCY, VehiclePropertyIds.SEAT_BELT_BUCKLED)

    private val props = base + ev.keys + seat

    private fun item(id: Int) = when (id) {
        in ev -> "EV·A·"
        in seat -> "기획3·A·"
        else -> "A·"
    } + VehiclePropertyIds.toString(id)

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = board.value(
            item(value.propertyId) + if (value.propertyId in seat) " area=0x${value.areaId.toString(16)}" else "",
            describe(value)
        )

        override fun onErrorEvent(propId: Int, zone: Int) =
            board.error(item(propId), "onErrorEvent zone=$zone")
    }

    fun start() {
        props.forEach { board.declare(item(it)) }
        val m = try {
            val c = Car.createCar(appContext)
            car = c
            c.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        } catch (t: Throwable) {
            props.forEach { board.error(item(it), t) }
            return
        }
        cpm = m
        props.forEach { probe(m, it) }
    }

    fun readAgain() {
        val m = cpm ?: return
        props.forEach { id ->
            val cfg = runCatching { m.getCarPropertyConfig(id) }.getOrNull() ?: return@forEach
            read(m, id, cfg)
        }
    }

    private fun probe(m: CarPropertyManager, id: Int) {
        val name = item(id)
        // 권한이 없으면 getCarPropertyConfig가 null을 돌려줄 수 있다 → "미지원"과 구분이 안 되므로 그대로 적는다
        val cfg = try {
            m.getCarPropertyConfig(id)
        } catch (t: Throwable) {
            board.error(name, t); return
        }
        if (cfg == null) {
            board.support(name, "config=null (미지원 또는 권한 없음)")
            return
        }
        board.support(
            name,
            "지원 access=${accessName(cfg.access)} change=${changeName(cfg.changeMode)} " +
                    "areas=${cfg.areaIds.joinToString()} rate=${cfg.minSampleRate}~${cfg.maxSampleRate}"
        )
        read(m, id, cfg)

        val rate = if (cfg.changeMode == CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_CONTINUOUS)
            CarPropertyManager.SENSOR_RATE_FASTEST else CarPropertyManager.SENSOR_RATE_ONCHANGE
        try {
            if (!m.registerCallback(callback, id, rate)) board.error(name, "registerCallback=false")
        } catch (t: Throwable) {
            board.error(name, t)
        }
    }

    private fun read(m: CarPropertyManager, id: Int, cfg: CarPropertyConfig<*>) {
        val name = item(id)
        if (cfg.access == CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_WRITE ||
            cfg.access == CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_NONE
        ) {
            board.error(name, "읽기 불가 access=${accessName(cfg.access)}")
            return
        }
        val areas = when {
            cfg.isGlobalProperty -> listOf(0)
            id in seat -> cfg.areaIds.toList()
            else -> listOf(cfg.areaIds.firstOrNull() ?: 0)
        }
        for (area in areas) {
            val label = if (id in seat) "$name area=0x${area.toString(16)}" else name
            try {
                board.value(label, describe(m.getProperty<Any>(id, area)))
            } catch (t: Throwable) {
                board.error(label, t)
            }
        }
    }

    private fun describe(v: CarPropertyValue<*>): String {
        val raw = v.value
        val shown = when {
            v.propertyId == VehiclePropertyIds.PERF_VEHICLE_SPEED && raw is Float ->
                "%.2f m/s (%.1f km/h)".format(raw, raw * 3.6f)
            else -> ProbeBoard.fmt(raw) + (ev[v.propertyId]?.takeIf { it.isNotEmpty() }?.let { " $it" } ?: "")
        }
        return "$shown area=${v.areaId} status=${v.status}"
    }

    private fun accessName(a: Int) = when (a) {
        CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_NONE -> "NONE"
        CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ -> "READ"
        CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_WRITE -> "WRITE"
        CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ_WRITE -> "READ_WRITE"
        else -> "$a"
    }

    private fun changeName(c: Int) = when (c) {
        CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_STATIC -> "STATIC"
        CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_ONCHANGE -> "ONCHANGE"
        CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_CONTINUOUS -> "CONTINUOUS"
        else -> "$c"
    }

    fun stop() {
        runCatching { cpm?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
    }
}
