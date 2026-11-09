package com.tracers.trace.domain

/**
 * 운전 판정·점수 기준값 (기획안 B-04, E-01, E-02). 급가속·급감속을 쓰는 곳(전비 C-01, 거칠었음 B-04,
 * 안전점수 E-01)은 모두 이 값을 쓴다. 기준을 바꿀 때는 여기만 고친다.
 */
object DrivingThresholds {
    // ---------- 이벤트 판정 ----------

    /** 급가속: 차속이 1초에 이만큼 이상 오름 (km/h per 초) */
    const val HARD_ACCEL_KMH_PER_SEC = 11.0

    /** 급감속: 차속이 1초에 이만큼 이상 내림 (km/h per 초, 양수로 표기) */
    const val HARD_DECEL_KMH_PER_SEC = 7.5

    /** 급정거: 차속이 1초에 이만큼 이상 내림 (km/h per 초). 급감속과 겹쳐 세지 않는다 */
    const val HARD_STOP_KMH_PER_SEC = 11.0

    /** 과속: NaviHelper 제한속도보다 이만큼 넘게 빠르면 (km/h) */
    const val SPEEDING_MARGIN_KMH = 10.0

    /** 과속: 초과가 이만큼 이어지면 1회 (ms) */
    const val SPEEDING_HOLD_MS = 10_000L

    // ---------- 거칠었음 (B-04) ----------

    /** 최근 이 시간 안의 급가속·급감속을 센다 (ms) */
    const val ROUGH_WINDOW_MS = 120_000L

    /** 이 횟수 이상이면 거칠었음 */
    const val ROUGH_MIN_EVENTS = 2

    // ---------- 1km 점수 (E-01) ----------

    /** 1km 구간 길이 (km) */
    const val SEGMENT_KM = 1.0

    /** 이벤트 없는 1km 점수 = 깨끗한 1km */
    const val KM_SCORE_CLEAN = 100

    const val PENALTY_HARD_ACCEL = 20
    const val PENALTY_HARD_DECEL = 20
    const val PENALTY_HARD_STOP = 30
    const val PENALTY_SPEEDING = 20

    // ---------- 안전점수·코인 (E-01, E-02) ----------

    /** 안전점수 시작값 */
    const val SAFETY_START = 85.0

    /** 안전점수 = 기존 × (1 − w) + 이번 1km × w */
    const val SAFETY_KM_WEIGHT = 0.02

    /** 안전점수가 이 값 이상일 때만 깨끗한 1km마다 코인 적립 */
    const val COIN_ACCRUAL_MIN_SCORE = 80.0

    /** 깨끗한 1km당 코인 */
    const val COINS_PER_CLEAN_KM = 1

    // ---------- 차속 잡음 ----------

    /** 이 시간 안에 튀었다 돌아오는 값 하나는 잡음으로 버린다 (ms) */
    const val SPIKE_WINDOW_MS = 300L

    /** 직전 값과 이만큼 이상 차이 날 때만 튐 후보로 본다 (km/h). 100ms 간격에서 3km/h = 초당 30km/h라 실제 운전으로는 나오지 않음 */
    const val SPIKE_MIN_JUMP_KMH = 3.0
}
