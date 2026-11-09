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
}
