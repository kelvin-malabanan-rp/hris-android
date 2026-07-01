package io.rocketpartners.hris.feature.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Builds the weeks (rows of 7 civil days) covering a month, aligned to [firstDayOfWeek]. */
object MonthGrid {
    fun weeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate>> {
        val first = month.atDay(1)
        val lead = ((first.dayOfWeek.value - firstDayOfWeek.value) + 7) % 7
        val gridStart = first.minusDays(lead.toLong())
        val last = month.atEndOfMonth()
        val trail = ((firstDayOfWeek.value + 6 - last.dayOfWeek.value) + 7) % 7
        val gridEnd = last.plusDays(trail.toLong())

        val weeks = mutableListOf<List<LocalDate>>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            weeks.add((0 until 7).map { cursor.plusDays(it.toLong()) })
            cursor = cursor.plusDays(7)
        }
        return weeks
    }

    /** Short weekday headers rotated to honor [firstDayOfWeek], e.g. ["Sun","Mon",...]. */
    fun weekdaySymbols(firstDayOfWeek: DayOfWeek, locale: Locale = Locale.getDefault()): List<String> =
        (0 until 7).map {
            firstDayOfWeek.plus(it.toLong()).getDisplayName(TextStyle.SHORT_STANDALONE, locale)
        }
}
