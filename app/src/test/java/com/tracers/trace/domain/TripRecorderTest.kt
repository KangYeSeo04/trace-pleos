package com.tracers.trace.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRecorderTest {

    private fun kmh(v: Double) = (v / 3.6).toFloat()

    private fun sample(
        t: Long, speedKmh: Double = 0.0, kwh: Double? = 60.0, odo: Double? = 1000.0,
        ac: Boolean? = false, pct: Int? = 80,
    ) = VehicleSample(
        timeMs = t, speedMps = kmh(speedKmh), batteryKwh = kwh,
        batteryPercent = pct, odometerKm = odo, hvacAcOn = ac,
    )

    @Test
    fun `PARKED에서 DRIVING이 되면 시작, 다시 PARKED가 되면 요약 하나를 낸다`() {
        val r = TripRecorder()
        r.onSample(sample(0))
        assertNull(r.onDriveState(DriveState.DRIVING, 0))
        r.onSample(sample(1_000, speedKmh = 30.0, kwh = 60.0, odo = 1000.0, pct = 80))
        r.onSample(sample(600_000, speedKmh = 30.0, kwh = 58.0, odo = 1012.0, pct = 77))
        val trip = r.onDriveState(DriveState.PARKED, 600_000)

        assertNotNull(trip)
        trip!!
        assertEquals(0L, trip.startedAtMs)
        assertEquals(600_000L, trip.endedAtMs)
        assertEquals(12.0, trip.distanceKm, 1e-6)
        assertEquals(2.0, trip.energyUsedKwh, 1e-6)
        assertEquals(80, trip.startBatteryPercent)
        assertEquals(77, trip.endBatteryPercent)
    }

    @Test
    fun `신호등 정차(STOPPED)는 주행을 끝내지 않는다`() {
        val r = TripRecorder()
        r.onSample(sample(0))
        r.onDriveState(DriveState.DRIVING, 0)
        r.onSample(sample(60_000, odo = 1002.0))
        assertNull(r.onDriveState(DriveState.STOPPED, 60_000))
        r.onDriveState(DriveState.DRIVING, 90_000)
        r.onSample(sample(120_000, odo = 1004.0))
        val trip = r.onDriveState(DriveState.PARKED, 120_000)!!
        assertEquals(4.0, trip.distanceKm, 1e-6)
        assertEquals(0L, trip.startedAtMs)
    }

    @Test
    fun `주행 중 배터리가 오르면 회생으로 따로 쌓는다`() {
        val r = TripRecorder()
        r.onSample(sample(0, kwh = 60.0))
        r.onDriveState(DriveState.DRIVING, 0)
        r.onSample(sample(10_000, kwh = 59.0, odo = 1001.0))
        r.onSample(sample(20_000, kwh = 59.3, odo = 1002.0)) // 회생 +0.3
        r.onSample(sample(30_000, kwh = 58.5, odo = 1003.0))
        val trip = r.onDriveState(DriveState.PARKED, 30_000)!!
        assertEquals(1.0 + 0.8, trip.energyUsedKwh, 1e-6)
        assertEquals(0.3, trip.regenKwh, 1e-6)
        assertEquals(1.5, trip.netEnergyKwh, 1e-6)
    }

    @Test
    fun `공조 켠 시간을 초 단위로 쌓는다`() {
        val r = TripRecorder()
        r.onSample(sample(0, ac = false))
        r.onDriveState(DriveState.DRIVING, 0)
        r.onSample(sample(10_000, ac = true, odo = 1001.0))
        r.onSample(sample(40_000, ac = false, odo = 1002.0)) // 10s~40s 켜짐 = 30초
        r.onSample(sample(50_000, ac = false, odo = 1003.0))
        val trip = r.onDriveState(DriveState.PARKED, 50_000)!!
        assertEquals(30L, trip.hvacOnSec)
    }

    @Test
    fun `1초에 11kmh 이상 빨라지면 급가속 1회, 계속 가속해도 한 번만 센다`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 0.0))
        r.onDriveState(DriveState.DRIVING, 0)
        // 0 → 12 → 24 → 36 km/h, 1초 간격: 한 번의 급가속
        r.onSample(sample(1_000, speedKmh = 12.0))
        r.onSample(sample(2_000, speedKmh = 24.0))
        r.onSample(sample(3_000, speedKmh = 36.0))
        // 정속
        r.onSample(sample(4_000, speedKmh = 36.0))
        // 정확히 기준값(+11)도 급가속
        r.onSample(sample(5_000, speedKmh = 47.0))
        // 기준 미만(+10)은 세지 않음
        r.onSample(sample(6_000, speedKmh = 57.0))
        r.onSample(sample(7_000, speedKmh = 46.0, odo = 1001.0))
        val trip = r.onDriveState(DriveState.PARKED, 7_000)!!
        assertEquals(2, trip.hardAccelCount)
    }

    @Test
    fun `100ms 간격 샘플도 1초 창으로 판단한다`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 0.0))
        r.onDriveState(DriveState.DRIVING, 0)
        // 0.1초마다 0.5 km/h씩 → 1초에 5 km/h: 급가속 아님 (기준 11)
        for (i in 1..30) r.onSample(sample(i * 100L, speedKmh = i * 0.5))
        r.onSample(sample(3_100, speedKmh = 15.0, odo = 1001.0))
        val trip = r.onDriveState(DriveState.PARKED, 3_100)!!
        assertEquals(0, trip.hardAccelCount)
    }

    @Test
    fun `총 주행거리가 없으면 차속을 적분해 거리를 대체 계산한다`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 36.0, odo = null))
        r.onDriveState(DriveState.DRIVING, 0)
        // 36km/h = 10 m/s, 1초 간격 100번 = 1km. 간격이 5초를 넘는 구간은 버린다
        for (i in 1..100) r.onSample(sample(i * 1_000L, speedKmh = 36.0, odo = null))
        val trip = r.onDriveState(DriveState.PARKED, 100_000)!!
        assertEquals(1.0, trip.distanceKm, 1e-3)
    }

    @Test
    fun `100m 미만 주행(주차 이동 등)은 버린다`() {
        val r = TripRecorder()
        r.onSample(sample(0, odo = 1000.0))
        r.onDriveState(DriveState.DRIVING, 0)
        r.onSample(sample(30_000, odo = 1000.05))
        assertNull(r.onDriveState(DriveState.PARKED, 30_000))
    }

    @Test
    fun `주행이 아닐 때 들어온 샘플은 쌓지 않는다`() {
        val r = TripRecorder()
        r.onSample(sample(0, kwh = 60.0, odo = 1000.0))
        r.onSample(sample(10_000, kwh = 50.0, odo = 1000.0))
        r.onDriveState(DriveState.DRIVING, 10_000)
        r.onSample(sample(20_000, kwh = 49.0, odo = 1001.0))
        val trip = r.onDriveState(DriveState.PARKED, 20_000)!!
        assertEquals(1.0, trip.energyUsedKwh, 1e-6)
        assertEquals(1.0, trip.distanceKm, 1e-6)
    }

    @Test
    fun `잔량 kWh는 Wh가 있으면 Wh, 없으면 용량 곱하기 %`() {
        assertEquals(58.0, BatteryEnergy.kwh(58_000f, 80, 77_400f)!!, 1e-6)
        assertEquals(61.92, BatteryEnergy.kwh(null, 80, 77_400f)!!, 1e-6)
        assertNull(BatteryEnergy.kwh(null, 80, null))
    }

    @Test
    fun `진행 중 스냅샷은 마지막으로 받은 값까지의 요약이고 기록은 계속된다`() {
        val r = TripRecorder()
        assertNull(r.snapshot())
        r.onSample(sample(0, kwh = 60.0, odo = 1000.0))
        r.onDriveState(DriveState.DRIVING, 0)
        r.onSample(sample(60_000, kwh = 59.0, odo = 1005.0))

        val snap = r.snapshot()!!
        assertEquals(60_000L, snap.endedAtMs)
        assertEquals(5.0, snap.distanceKm, 1e-6)
        assertEquals(1.0, snap.energyUsedKwh, 1e-6)

        r.onSample(sample(120_000, kwh = 58.0, odo = 1010.0))
        val trip = r.onDriveState(DriveState.PARKED, 120_000)!!
        assertEquals(10.0, trip.distanceKm, 1e-6)
        assertNull(r.snapshot())
    }

    @Test
    fun `앱이 다시 켜졌을 때 남은 스냅샷은 거리 기준을 넘으면 주행으로 마무리한다`() {
        val snap = TripRecord(0, 60_000, 5.0, 1.0, 0, 0.0, 0, 80, 79)
        assertEquals(snap, TripRecorder.finalizeRecovered(snap))
        assertNull(TripRecorder.finalizeRecovered(snap.copy(distanceKm = 0.05)))
    }

    @Test
    fun `주행 판정 2초 지연 동안의 출발 가속과 거리도 주행에 넣는다`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 0.0, kwh = 60.0, odo = 1000.0))
        // 차가 움직이기 시작 (아직 DRIVING 판정 전)
        r.onSample(sample(1_000, speedKmh = 35.0, kwh = 59.9, odo = 1000.01))
        r.onSample(sample(2_000, speedKmh = 40.0, kwh = 59.8, odo = 1000.02))
        r.onSample(sample(3_000, speedKmh = 40.0, kwh = 59.7, odo = 1000.03))
        r.onDriveState(DriveState.DRIVING, 3_000) // 2초 유지 뒤 판정
        r.onSample(sample(60_000, speedKmh = 40.0, kwh = 59.0, odo = 1001.0))
        val trip = r.onDriveState(DriveState.PARKED, 60_000)!!

        assertEquals(1_000L, trip.startedAtMs)
        assertEquals(1.0, trip.distanceKm, 1e-6)
        assertEquals(1.0, trip.energyUsedKwh, 1e-6)
        assertEquals(1, trip.hardAccelCount)
    }

    @Test
    fun `움직이다 판정 전에 다시 멈추면 후보를 버린다`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 0.0, kwh = 60.0, odo = 1000.0))
        r.onSample(sample(500, speedKmh = 3.0, kwh = 59.0, odo = 1000.0))
        r.onSample(sample(1_000, speedKmh = 0.0, kwh = 59.0, odo = 1000.0))
        r.onDriveState(DriveState.DRIVING, 5_000)
        r.onSample(sample(6_000, speedKmh = 20.0, kwh = 58.5, odo = 1000.5))
        val trip = r.onDriveState(DriveState.PARKED, 6_000)!!
        assertEquals(5_000L, trip.startedAtMs)
        assertEquals(0.5, trip.energyUsedKwh, 1e-6)
    }

    @Test
    fun `차속이 바뀔 때만 오는 신호도 1초 전 값과 비교한다 (정차 4초 뒤 0에서 40)`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 30.0))
        r.onDriveState(DriveState.DRIVING, 0)
        r.onSample(sample(1_000, speedKmh = 0.0, odo = 1001.0)) // 신호 대기 (감속은 급가속 아님)
        // 0 km/h 동안 이벤트 없음, 4.1초 뒤 40 km/h → 1초 전에는 0이었으므로 +40/s
        r.onSample(sample(5_100, speedKmh = 40.0))
        r.onSample(sample(5_200, speedKmh = 40.0))
        r.onSample(sample(6_300, speedKmh = 40.0, odo = 1002.0))
        val trip = r.onDriveState(DriveState.PARKED, 6_300)!!
        assertEquals(1, trip.hardAccelCount)
    }

    @Test
    fun `공조 시간은 주행 시작 이후만 센다 (주행 시간을 넘지 않는다)`() {
        val r = TripRecorder()
        r.onSample(sample(0, speedKmh = 0.0, ac = true))           // 정차 중 공조 켬
        r.onSample(sample(10_000, speedKmh = 30.0, ac = true))     // 10초 뒤 출발
        r.onDriveState(DriveState.DRIVING, 12_000)
        r.onSample(sample(40_000, speedKmh = 30.0, ac = true, odo = 1001.0))
        val trip = r.onDriveState(DriveState.PARKED, 40_000)!!
        assertEquals(10_000L, trip.startedAtMs)
        assertEquals(30L, trip.hvacOnSec)
        assertTrue(trip.hvacOnSec <= trip.durationSec)
    }
}
