package io.rocketpartners.hris.feature.calendar

import io.rocketpartners.hris.designsystem.LeaveDateText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthGridTest {

    @Test
    fun weeks_coverMonthAlignedToFirstDayOfWeek() {
        // June 2026: 1st is a Monday. Sunday-start grid → first cell is Sun May 31.
        val weeks = MonthGrid.weeks(YearMonth.of(2026, 6), DayOfWeek.SUNDAY)
        assertEquals(7, weeks.first().size)
        assertEquals(LocalDate.of(2026, 5, 31), weeks.first().first())
        assertEquals(DayOfWeek.SUNDAY, weeks.first().first().dayOfWeek)
        // Grid fully contains June and ends on a Saturday.
        assertTrue(weeks.flatten().contains(LocalDate.of(2026, 6, 30)))
        assertEquals(DayOfWeek.SATURDAY, weeks.last().last().dayOfWeek)
    }

    @Test
    fun weeks_mondayStartRotatesLeadIn() {
        val weeks = MonthGrid.weeks(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        // June 1 2026 is Monday → Monday-start grid begins exactly on June 1.
        assertEquals(LocalDate.of(2026, 6, 1), weeks.first().first())
    }

    @Test
    fun leaveDateText_formatsRanges() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals("Jun 5", LeaveDateText.range("2026-06-05", "2026-06-05", today))
        assertEquals("Jun 22–24", LeaveDateText.range("2026-06-22", "2026-06-24", today))
        assertEquals("Jun 5, 2025", LeaveDateText.range("2025-06-05", "2025-06-05", today))
    }
}
