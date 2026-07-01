package io.rocketpartners.hris.designsystem

import io.rocketpartners.hris.core.networking.WireDate
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats a leave's wire date range (`yyyy-MM-dd`) into friendly text. Mirrors iOS `LeaveDateText`. */
object LeaveDateText {
    private val MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    private val MONTH_DAY_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US)

    fun range(start: String?, end: String?, today: LocalDate = LocalDate.now()): String {
        val s = start?.let(WireDate::parse)?.let { LocalDate.ofInstant(it, ZoneOffset.UTC) }
        val e = end?.let(WireDate::parse)?.let { LocalDate.ofInstant(it, ZoneOffset.UTC) }
        return when {
            s != null && e != null -> {
                if (s == e) {
                    medium(s, today)
                } else if (s.year == e.year && s.month == e.month) {
                    val suffix = if (s.year == today.year) "" else ", ${s.year}"
                    "${MONTH.format(s)} ${s.dayOfMonth}–${e.dayOfMonth}$suffix"
                } else {
                    "${medium(s, today)} – ${medium(e, today)}"
                }
            }
            s != null -> medium(s, today)
            e != null -> medium(e, today)
            else -> start ?: end ?: "—"
        }
    }

    private fun medium(date: LocalDate, today: LocalDate): String =
        if (date.year == today.year) MONTH_DAY.format(date) else MONTH_DAY_YEAR.format(date)
}
