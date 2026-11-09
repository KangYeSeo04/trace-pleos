package com.tracers.trace.domain.region

import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 위치를 어느 SDK에서 받을지 고른다 (G-01).
 *
 * - NaviHelper getCurrentLocationInfo가 먼저.
 * - 차가 움직이는데(차속 > 0) NaviHelper 위치가 [stuckAfterMs] 넘게 그대로면 FusedLocation으로 바꾼다
 *   (에뮬레이터 NaviHelper는 geo fix를 따르지 않고 한 자리만 답한다. 2026.11.09 실측).
 * - FusedLocation 중에도 NaviHelper는 계속 묻고, 그 위치가 움직이면 NaviHelper로 돌아온다.
 *
 * 순수 계산이라 시각은 밖에서 넣는다. 한 스레드에서만 부른다.
 */
class LocationSourceSelector(
    /** FusedLocation을 쓸 수 있는지 (권한·서비스). false면 NaviHelper만 */
    @Volatile var fusedAvailable: Boolean,
    private val stuckAfterMs: Long = STUCK_AFTER_MS,
    private val onSwitch: (Source) -> Unit = {},
) {
    enum class Source { NAVI, FUSED }

    var source: Source = Source.NAVI
        private set

    /** NaviHelper가 마지막으로 다른 자리를 답한 뒤, 차가 움직인 시간 */
    private var stuckMovingMs = 0L
    private var lastNavi: GeoFix? = null
    private var lastTickMs: Long? = null
    private var lastMoving = false

    /** 위치를 물을 때마다(주행 5초·주차 60초) 부른다. 그 사이 움직인 시간을 센다 */
    fun onTick(nowMs: Long, speedMps: Float?) {
        val prev = lastTickMs
        val wasMoving = lastMoving
        val moving = (speedMps ?: 0f) > 0f
        lastTickMs = nowMs
        lastMoving = moving
        if (prev == null) return
        // 앞뒤 두 번 모두 움직이던 구간만 센다 (정차 중 60초 간격이 주행 시간으로 들어가지 않게)
        if (moving && wasMoving) stuckMovingMs += (nowMs - prev).coerceIn(0, MAX_TICK_GAP_MS)
        if (source == Source.NAVI && fusedAvailable && stuckMovingMs > stuckAfterMs) switchTo(Source.FUSED)
    }

    /** NaviHelper 답. 그 값을 써야 하면 true */
    fun onNavi(fix: GeoFix): Boolean {
        if (fix.isValid) {
            val last = lastNavi
            if (last == null || distanceM(last, fix) > MOVED_M) {
                if (last != null) {
                    stuckMovingMs = 0
                    if (source == Source.FUSED) switchTo(Source.NAVI)
                }
                lastNavi = fix
            }
        }
        return source == Source.NAVI
    }

    /** FusedLocation 답. 그 값을 써야 하면 true */
    fun onFused(fix: GeoFix): Boolean = source == Source.FUSED && fix.isValid

    private fun switchTo(s: Source) {
        source = s
        onSwitch(s)
    }

    private fun distanceM(a: GeoFix, b: GeoFix): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude) * cos(Math.toRadians((a.latitude + b.latitude) / 2))
        return sqrt(dLat * dLat + dLon * dLon) * EARTH_M
    }

    companion object {
        const val STUCK_AFTER_MS = 60_000L
        /** 이보다 덜 바뀌면 "그대로" (GPS 흔들림) */
        private const val MOVED_M = 10.0
        /** 앱이 멈췄다 깨어난 긴 공백은 움직인 시간으로 세지 않는다 */
        private const val MAX_TICK_GAP_MS = 15_000L
        private const val EARTH_M = 6_371_000.0
    }
}
