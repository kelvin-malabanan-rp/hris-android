package io.rocketpartners.hris.core.networking

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WireDateTest {

    @Test
    fun parsesDateOnlyAtUtcMidnight() {
        val parsed = WireDate.parse("2026-03-15")
        assertEquals(LocalDate.of(2026, 3, 15).atStartOfDay(ZoneOffset.UTC).toInstant(), parsed)
    }

    @Test
    fun parsesIsoDatetimeWithZuluAndOffset() {
        assertEquals(Instant.parse("2026-03-15T08:30:00Z"), WireDate.parse("2026-03-15T08:30:00Z"))
        assertEquals(Instant.parse("2026-03-15T00:30:00Z"), WireDate.parse("2026-03-15T08:30:00+08:00"))
    }

    @Test
    fun offsetLessDatetimeAndGarbageReturnNull() {
        // Mirrors iOS: ISO8601(.withInternetDateTime) rejects offset-less LocalDateTime.
        assertNull(WireDate.parse("2026-03-15T08:30:00"))
        assertNull(WireDate.parse("not-a-date"))
    }

    @Test
    fun formatsDateOnlyAndMonthInUtc() {
        val instant = Instant.parse("2026-03-15T23:00:00Z")
        assertEquals("2026-03-15", WireDate.dateOnly(instant))
        assertEquals("2026-03", WireDate.month(instant))
    }
}
