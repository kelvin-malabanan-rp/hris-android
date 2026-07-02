package io.rocketpartners.hris.feature.wfh

import io.rocketpartners.hris.model.WfhSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WfhScheduleClassifierTest {

    @Test
    fun classify_splitsScheduledPendingAndSkipped() {
        val requested = listOf(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 8), LocalDate.of(2026, 7, 10))
        // Server created two of the three: one approved (within quota), one pending (over quota).
        // Jul 10 wasn't returned → skipped (past/duplicate).
        val response = listOf(
            WfhSchedule(id = 1, date = "2026-07-06", status = "APPROVED"),
            WfhSchedule(id = 2, date = "2026-07-08", status = "pending"),
        )
        val result = WfhScheduleClassifier.classify(requested, response)
        assertEquals(listOf(1), result.scheduled.map { it.id })
        assertEquals(listOf(2), result.pending.map { it.id })
        assertEquals(listOf(LocalDate.of(2026, 7, 10)), result.skipped)
    }

    @Test
    fun classify_allSkippedWhenResponseEmpty() {
        val requested = listOf(LocalDate.of(2026, 7, 1))
        val result = WfhScheduleClassifier.classify(requested, emptyList())
        assertEquals(requested, result.skipped)
        assertEquals(0, result.scheduled.size + result.pending.size)
    }
}
