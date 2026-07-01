package io.rocketpartners.hris.core.networking

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Formats and parses the backend's wire dates. The API uses `yyyy-MM-dd` for all-day values and
 * ISO-8601 datetimes elsewhere, so parsing tolerates both. All formatting is UTC.
 *
 * Mirrors the iOS `WireDate` enum: [parse] tries date-only first, then an internet-datetime with an
 * offset (e.g. `...Z` / `+08:00`). Offset-less `LocalDateTime` strings return null — matching iOS,
 * whose `ISO8601DateFormatter(.withInternetDateTime)` also rejects them.
 */
object WireDate {
    /** `yyyy-MM-dd` in UTC — the format the API's date query params expect. */
    fun dateOnly(instant: Instant): String =
        LocalDate.ofInstant(instant, ZoneOffset.UTC).toString()

    /** `yyyy-MM` in UTC — the format the WFH `month` query param expects. */
    fun month(instant: Instant): String =
        YearMonth.from(LocalDate.ofInstant(instant, ZoneOffset.UTC)).toString()

    /** Parses a wire date string, trying date-only then ISO datetime. Returns null on failure. */
    fun parse(string: String): Instant? {
        runCatching { LocalDate.parse(string) }.getOrNull()
            ?.let { return it.atStartOfDay(ZoneOffset.UTC).toInstant() }
        runCatching { Instant.parse(string) }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(string).toInstant() }.getOrNull()?.let { return it }
        return null
    }

    /** UTC civil day (midnight-anchored instant) for [instant] — used for timezone-safe day matching. */
    fun civilDay(instant: Instant): LocalDate = LocalDate.ofInstant(instant, ZoneOffset.UTC)
}
