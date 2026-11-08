package com.tracers.trace.domain

import com.tracers.trace.data.vehicle.Gear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DriveStateDetector] 단위 테스트.
 *
 * delayMs=0으로 즉시 상태 전환하도록 설정하여 테스트한다.
 * 실제 2초 지연은 에뮬레이터에서 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DriveStateDetectorTest {

    @Test
    fun `P단이면 PARKED`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = DriveStateDetector(scope, delayMs = 0)

        detector.update(Gear.PARK, 0f)
        testScheduler.advanceUntilIdle()

        assertEquals(DriveState.PARKED, detector.currentState.value)
        scope.cancel()
    }

    @Test
    fun `D단 + 속도 0이면 STOPPED`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = DriveStateDetector(scope, delayMs = 0)

        detector.update(Gear.DRIVE, 0f)
        testScheduler.advanceUntilIdle()

        assertEquals(DriveState.STOPPED, detector.currentState.value)
        scope.cancel()
    }

    @Test
    fun `D단 + 속도 10이면 DRIVING`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = DriveStateDetector(scope, delayMs = 0)

        detector.update(Gear.DRIVE, 10f)
        testScheduler.advanceUntilIdle()

        assertEquals(DriveState.DRIVING, detector.currentState.value)
        scope.cancel()
    }

    @Test
    fun `상태 판정 로직이 올바름`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(testDispatcher)
        val detector = DriveStateDetector(scope, delayMs = 0)

        // PARK → PARKED
        detector.update(Gear.PARK, 0f)
        testScheduler.advanceUntilIdle()
        assertEquals(DriveState.PARKED, detector.currentState.value)

        // DRIVE + 0 → STOPPED
        detector.update(Gear.DRIVE, 0f)
        testScheduler.advanceUntilIdle()
        assertEquals(DriveState.STOPPED, detector.currentState.value)

        // DRIVE + 10 → DRIVING
        detector.update(Gear.DRIVE, 10f)
        testScheduler.advanceUntilIdle()
        assertEquals(DriveState.DRIVING, detector.currentState.value)

        // REVERSE + 5 → DRIVING
        detector.update(Gear.REVERSE, 5f)
        testScheduler.advanceUntilIdle()
        assertEquals(DriveState.DRIVING, detector.currentState.value)

        // NEUTRAL + 0 → STOPPED
        detector.update(Gear.NEUTRAL, 0f)
        testScheduler.advanceUntilIdle()
        assertEquals(DriveState.STOPPED, detector.currentState.value)

        scope.cancel()
    }
}
