package com.tracers.trace.probe

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/** 기획 검증 2: 위아래 흔들림(과속방지턱)용 센서 목록 + 가속도 1초 샘플 */
class SensorProbe(context: Context, private val board: ProbeBoard) {

    private val sm = context.applicationContext.getSystemService(SensorManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var listener: SensorEventListener? = null

    fun start() {
        listOf(LIST, ACCEL).forEach(board::declare)
        val all = sm?.getSensorList(Sensor.TYPE_ALL).orEmpty()
        board.value(LIST, "${all.size}개: " + all.joinToString(" / ") { "${it.name}|${it.vendor}|${it.stringType}" })
        // 표 한 칸에 다 안 들어가므로 전체는 로그로 한 줄씩
        all.forEach { Log.i("PROBE", "${System.currentTimeMillis()} · 기획2·센서 · ${it.name} | ${it.vendor} | type=${it.type} ${it.stringType} | v${it.version}") }
        sample()
    }

    fun readAgain() = sample()

    /** 가속도 센서 1초 동안 받아서 축별 최소~최대와 개수만 표시 */
    private fun sample() {
        val s = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (s == null) {
            board.support(ACCEL, "TYPE_ACCELEROMETER 없음"); return
        }
        board.support(ACCEL, "${s.name} | ${s.vendor} | 가상=${isVirtual(s)}")
        stopListening()
        val min = FloatArray(3) { Float.MAX_VALUE }
        val max = FloatArray(3) { -Float.MAX_VALUE }
        var count = 0
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                count++
                for (i in 0..2) { min[i] = minOf(min[i], e.values[i]); max[i] = maxOf(max[i], e.values[i]) }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        listener = l
        if (!sm.registerListener(l, s, SensorManager.SENSOR_DELAY_GAME)) {
            board.error(ACCEL, "registerListener=false"); return
        }
        main.postDelayed({
            stopListening()
            val axes = listOf("x", "y", "z").mapIndexed { i, a -> "$a=%.3f~%.3f".format(min[i], max[i]) }
            board.value(ACCEL, "1초 ${count}개 " + if (count == 0) "(값 없음)" else axes.joinToString(" "))
        }, 1_000)
    }

    // 에뮬레이터 센서는 이름·제조사에 Goldfish/AOSP/"Emulator"가 들어간다 (판정 근거는 이름·제조사 문자열뿐)
    private fun isVirtual(s: Sensor) =
        listOf("goldfish", "emulator", "ranchu", "aosp", "the android open source project")
            .any { s.name.lowercase().contains(it) || s.vendor.lowercase().contains(it) }

    private fun stopListening() {
        listener?.let { sm?.unregisterListener(it) }
        listener = null
    }

    fun stop() {
        main.removeCallbacksAndMessages(null)
        stopListening()
    }

    companion object {
        const val LIST = "기획2·센서 목록 getSensorList(TYPE_ALL)"
        const val ACCEL = "기획2·가속도 TYPE_ACCELEROMETER 1초"
    }
}
