package io.rocketpartners.hris.feature.notifications

import io.rocketpartners.hris.model.AppNotification
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxGroupingTest {

    // Fixed "now": 2026-07-02T12:00:00Z, bucketed in UTC for deterministic civil-day math.
    private val now: Instant = Instant.parse("2026-07-02T12:00:00Z")
    private val zone = ZoneOffset.UTC

    private fun notif(id: Int, daysAgo: Long, hoursAgo: Long = 0, read: Boolean = false, type: String = "LEAVE_APPROVED"): AppNotification {
        val created = DateTimeFormatter.ISO_INSTANT.format(now.minus(java.time.Duration.ofDays(daysAgo).plusHours(hoursAgo)))
        return AppNotification(id = id, type = type, title = "t$id", message = "m$id", isRead = read, createdAt = created)
    }

    @Test
    fun groups_bucketByCivilDay() {
        val items = listOf(
            notif(1, daysAgo = 0, hoursAgo = 2),   // Today
            notif(2, daysAgo = 1),                  // Yesterday
            notif(3, daysAgo = 3),                  // Last 7 days
            notif(4, daysAgo = 20),                 // Earlier
        )
        val groups = InboxGrouping.groups(items, now = now, zone = zone)
        assertEquals(listOf("Today", "Yesterday", "Last 7 days", "Earlier"), groups.map { it.title })
        assertEquals(listOf(1), groups[0].items.map { it.id })
        assertEquals(listOf(2), groups[1].items.map { it.id })
        assertEquals(listOf(3), groups[2].items.map { it.id })
        assertEquals(listOf(4), groups[3].items.map { it.id })
    }

    @Test
    fun groups_omitsEmptyBucketsAndKeepsOrderWithinBucket() {
        val items = listOf(notif(1, 0, 1), notif(2, 0, 5), notif(3, 20))
        val groups = InboxGrouping.groups(items, now = now, zone = zone)
        assertEquals(listOf("Today", "Earlier"), groups.map { it.title })
        assertEquals(listOf(1, 2), groups[0].items.map { it.id }) // preserves input order
    }

    @Test
    fun groups_futureDatedNotificationBucketsAsToday() {
        // Clock skew: created 6 hours in the "future" must not sink to Earlier.
        val future = DateTimeFormatter.ISO_INSTANT.format(now.plus(6, ChronoUnit.HOURS))
        val groups = InboxGrouping.groups(
            listOf(AppNotification(id = 9, type = "LEAVE_APPROVED", title = "t", message = "m", createdAt = future)),
            now = now, zone = zone,
        )
        assertEquals(listOf("Today"), groups.map { it.title })
    }

    @Test
    fun groups_sixDaysAgoIsLast7Days_sevenDaysAgoIsEarlier() {
        val groups = InboxGrouping.groups(listOf(notif(1, 6), notif(2, 7)), now = now, zone = zone)
        assertEquals("Last 7 days", groups.first { 1 in it.items.map { n -> n.id } }.title)
        assertEquals("Earlier", groups.first { 2 in it.items.map { n -> n.id } }.title)
    }

    @Test
    fun groups_unparseableDateFallsToEarlier() {
        val bad = AppNotification(id = 1, type = "OTHER", title = "t", message = "m", createdAt = "not-a-date")
        val groups = InboxGrouping.groups(listOf(bad), now = now, zone = zone)
        assertEquals(listOf("Earlier"), groups.map { it.title })
    }

    @Test
    fun groups_emptyInputYieldsNoSections() {
        assertTrue(InboxGrouping.groups(emptyList(), now = now, zone = zone).isEmpty())
    }
}
