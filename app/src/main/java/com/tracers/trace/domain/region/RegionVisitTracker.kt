package com.tracers.trace.domain.region

import com.tracers.trace.domain.SpeedReading

/**
 * 저장할 방문 기록 증가분 하나 (161 지역 단위).
 *
 * @property atMs 이 지역에 마지막으로 있었던 시각 (처음 간 날·마지막 간 날)
 */
data class RegionVisitDelta(
    val regionCode: String,
    val distanceKm: Double,
    val stayMs: Long,
    val atMs: Long,
)

/**
 * 지금 지역에 달린 거리·머문 시간을 쌓는다 (G-03).
 *
 * - 거리: 걸러진 차속을 적분해 그 순간의 지역에 넣는다 (KmSegmenter와 같은 방식, [MAX_SPEED_GAP_MS] 넘게 끊긴 간격은 뺀다)
 * - 머문 시간: 같은 지역 위치를 연달아 받은 간격. 경계를 넘으면 그 간격은 앞 지역 몫이다.
 *   [MAX_STAY_GAP_MS]보다 긴 간격은 앱이 꺼져 있었거나 위치가 끊긴 것으로 보고 넣지 않는다
 * - 지역을 모를 때(위치 못 받음·지도 밖)는 쌓지 않는다
 *
 * 원시 값은 바로 버리고 지역별 합만 남긴다. [drain]으로 꺼내 저장한다. 스레드 안전하지 않다.
 */
class RegionVisitTracker {

    var currentRegion: String? = null
        private set
    private var lastFixMs: Long? = null
    private var prevSpeed: SpeedReading? = null
    private val pending = LinkedHashMap<String, Acc>()

    private class Acc(var meters: Double = 0.0, var stayMs: Long = 0, var atMs: Long = 0)

    /** 위치 판정 하나. [code] null = 지역 모름 */
    fun onRegion(code: String?, timeMs: Long) {
        val cur = currentRegion
        val lf = lastFixMs
        if (cur != null && lf != null) {
            val dt = timeMs - lf
            if (dt in 1..MAX_STAY_GAP_MS) acc(cur, timeMs).stayMs += dt
        }
        currentRegion = code
        lastFixMs = if (code == null) null else timeMs
        if (code != null) acc(code, timeMs) // 처음 들어온 지역도 바로 방문으로 남긴다
    }

    /** 걸러진 차속 하나 */
    fun onSpeed(r: SpeedReading) {
        val p = prevSpeed
        prevSpeed = r
        val cur = currentRegion ?: return
        if (p == null) return
        val dt = r.timeMs - p.timeMs
        if (dt <= 0 || dt > MAX_SPEED_GAP_MS) return
        val m = p.speedMps.coerceAtLeast(0f) * dt / 1000.0
        if (m > 0) acc(cur, r.timeMs).meters += m
    }

    /** 쌓인 증가분을 꺼내고 비운다 */
    fun drain(): List<RegionVisitDelta> {
        if (pending.isEmpty()) return emptyList()
        val out = pending.map { (code, a) -> RegionVisitDelta(code, a.meters / 1000.0, a.stayMs, a.atMs) }
        pending.clear()
        return out
    }

    /** 위치 기록을 끄면: 지금 지역과 쌓인 값을 잊는다 */
    fun reset() {
        currentRegion = null
        lastFixMs = null
        prevSpeed = null
        pending.clear()
    }

    /** 기록을 지웠을 때 아직 저장 안 한 증가분도 버린다. null = 전부 */
    fun discard(code: String?) {
        if (code == null) pending.clear() else pending.remove(code)
    }

    private fun acc(code: String, atMs: Long): Acc =
        pending.getOrPut(code) { Acc() }.also { if (atMs > it.atMs) it.atMs = atMs }

    companion object {
        /** 차속 간격이 이보다 길면 끊긴 것 (KmSegmenter와 같음) */
        const val MAX_SPEED_GAP_MS = 5_000L
        /** 위치 간격이 이보다 길면 머문 시간에 넣지 않는다. 주차 중 요청 간격(60초)의 3배 */
        const val MAX_STAY_GAP_MS = 180_000L
    }
}
