package com.tracers.trace.data.uxrestrictions

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AAOS 시스템 주행 제한 상태 감지 (CarUxRestrictionsManager).
 *
 * requiresDistractionOptimization이 true가 되면 앱은 즉시 운전 중 안전한 화면(홈)으로 복귀해야 한다.
 * DriveStateDetector의 2초 지연과 무관하게, 시스템 판단과 우리 판단 중 먼저 오는 쪽을 따른다.
 */
class CarUxRestrictionsMonitor(context: Context) {

    private val appContext = context.applicationContext
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    private val _requiresDistractionOptimization = MutableStateFlow(false)
    val requiresDistractionOptimization: StateFlow<Boolean> = _requiresDistractionOptimization.asStateFlow()

    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
        val required = restrictions.isRequiresDistractionOptimization
        _requiresDistractionOptimization.value = required
        Log.d(TAG, "UX 제한: requiresDistractionOptimization=$required")
    }

    fun start() {
        runCatching {
            val c = Car.createCar(appContext)
            car = c
            val mgr = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            manager = mgr

            // 현재 상태 즉시 읽기
            val current = mgr.currentCarUxRestrictions
            _requiresDistractionOptimization.value = current?.isRequiresDistractionOptimization ?: false

            // 리스너 등록
            mgr.registerListener(listener)
            Log.d(TAG, "CarUxRestrictionsManager 구독 시작")
        }.onFailure {
            Log.w(TAG, "CarUxRestrictionsManager 구독 실패", it)
        }
    }

    fun stop() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
        car = null
        manager = null
    }

    companion object {
        private const val TAG = "CarUxRestrictions"
    }
}
