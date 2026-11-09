package com.tracers.trace.domain

/**
 * 운전 판정 기준값 (기획안 B-04). 급가속·급감속을 쓰는 곳(전비 C-01, 거칠었음 B-04, 안전점수 E-01)은
 * 모두 이 값을 쓴다.
 */
object DrivingThresholds {
    /** 급가속: 차속이 1초에 이만큼 이상 오름 (km/h per 초) */
    const val HARD_ACCEL_KMH_PER_SEC = 11.0

    /** 급감속: 차속이 1초에 이만큼 이상 내림 (km/h per 초, 양수로 표기). B-04에서 사용 */
    const val HARD_DECEL_KMH_PER_SEC = 7.5

    // ---------- 차속 잡음 ----------

    /** 이 시간 안에 튀었다 돌아오는 값 하나는 잡음으로 버린다 (ms) */
    const val SPIKE_WINDOW_MS = 300L

    /** 직전 값과 이만큼 이상 차이 날 때만 튐 후보로 본다 (km/h). 100ms 간격에서 3km/h = 초당 30km/h라 실제 운전으로는 나오지 않음 */
    const val SPIKE_MIN_JUMP_KMH = 3.0
}
