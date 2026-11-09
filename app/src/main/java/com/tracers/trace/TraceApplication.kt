package com.tracers.trace

import android.app.Application
import com.tracers.trace.data.permission.RuntimePermissions
import com.tracers.trace.data.record.CoinRepository
import com.tracers.trace.data.record.DriveRecordCollector
import com.tracers.trace.data.record.EfficiencySource
import com.tracers.trace.data.record.RecordRepository
import com.tracers.trace.data.record.TraceDatabase
import com.tracers.trace.data.vehicle.PleosVehicleSignals
import com.tracers.trace.data.vehicle.VehicleSignals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    lateinit var coinRepository: CoinRepository
        private set
    lateinit var recordCollector: DriveRecordCollector
        private set
    lateinit var efficiencySource: EfficiencySource
        private set

    private val _permissionsGranted = MutableStateFlow(false)

    /** 런타임 권한(CAR_SPEED 등)이 모두 허용됐는지. false면 수집을 시작하지 않고 홈에 안내 문구 */
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        vehicleSignals = PleosVehicleSignals(this)
        val db = TraceDatabase.create(this)
        recordRepository = RecordRepository(db)
        coinRepository = CoinRepository(db)
        recordCollector = DriveRecordCollector(vehicleSignals, recordRepository, coinRepository, appScope)
        efficiencySource = EfficiencySource(recordRepository, vehicleSignals)
        refreshPermissions()
    }

    /**
     * 권한 상태를 다시 확인하고, 모두 허용됐으면 수집을 시작한다(한 번만).
     * 앱 시작, 권한 요청 결과, 화면 복귀(설정에서 허용했을 수 있음) 때 부른다.
     */
    fun refreshPermissions() {
        val granted = RuntimePermissions.missing(this).isEmpty()
        _permissionsGranted.value = granted
        if (granted) recordCollector.start()
    }
}
