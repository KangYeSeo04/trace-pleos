package com.tracers.trace

import android.app.Application
import com.tracers.trace.data.record.DriveRecordCollector
import com.tracers.trace.data.record.RecordRepository
import com.tracers.trace.data.record.TraceDatabase
import com.tracers.trace.data.vehicle.PleosVehicleSignals
import com.tracers.trace.data.vehicle.VehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 앱 프로세스 범위의 단일 객체들. 차량 신호 구독과 기록 수집은 여기서 한 번만 시작한다.
 */
class TraceApplication : Application() {

    /** 프로세스와 함께 사는 범위. 한 줄로 순서대로 돌려 계산 객체를 동시에 건드리지 않게 한다 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    lateinit var vehicleSignals: VehicleSignals
        private set
    lateinit var recordRepository: RecordRepository
        private set
    lateinit var recordCollector: DriveRecordCollector
        private set

    override fun onCreate() {
        super.onCreate()
        vehicleSignals = PleosVehicleSignals(this)
        recordRepository = RecordRepository(TraceDatabase.create(this))
        recordCollector = DriveRecordCollector(vehicleSignals, recordRepository, appScope).also { it.start() }
    }
}
