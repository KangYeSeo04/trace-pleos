package com.tracers.trace.data.record

import com.tracers.trace.data.record.MaintenanceRepository.Companion.toDomain
import com.tracers.trace.data.record.MaintenanceRepository.Companion.toEntity
import com.tracers.trace.domain.MaintenanceEntry
import com.tracers.trace.domain.MaintenanceInputMethod
import com.tracers.trace.domain.MaintenanceItemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MaintenanceRepositoryMappingTest {

    @Test
    fun `저장 형태로 바꿨다 되돌리면 같은 기록`() {
        val e = MaintenanceEntry(
            id = 3, date = LocalDate.of(2026, 11, 9), odometerKm = 38_800, place = "정비소",
            items = setOf(MaintenanceItemType.TIRE_ROTATION, MaintenanceItemType.WIPER), other = true,
            costWon = 80_000, memo = "메모", method = MaintenanceInputMethod.AUTO_DETECTED,
        )
        val entity = e.toEntity(nowMs = 1L)
        assertEquals("WIPER,TIRE_ROTATION", entity.items)
        assertEquals(e, entity.toDomain())
    }

    @Test
    fun `모르는 항목 이름과 빈 항목은 버린다`() {
        val entity = MaintenanceRecordEntity(
            epochDay = 0, odometerKm = 0, place = "", items = "NOPE,COOLANT", other = false,
            costWon = null, memo = "", inputMethod = "??", createdAtMs = 0,
        )
        val d = entity.toDomain()
        assertEquals(setOf(MaintenanceItemType.COOLANT), d.items)
        assertEquals(MaintenanceInputMethod.MANUAL, d.method)
        assertEquals(emptySet<MaintenanceItemType>(), entity.copy(items = "").toDomain().items)
    }
}
