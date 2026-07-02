package io.rocketpartners.hris.feature.notifications

import io.rocketpartners.hris.model.AppNotification
import io.rocketpartners.hris.model.NotificationKind
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxFilterTest {

    private val now = Instant.parse("2026-07-02T12:00:00Z")

    private fun notif(type: String, read: Boolean = false) =
        AppNotification(id = 1, type = type, title = "t", message = "m", isRead = read, createdAt = "2026-07-02T10:00:00Z")

    @Test
    fun all_matchesEverything() {
        assertTrue(InboxFilter.ALL.matches(notif("LEAVE_APPROVED", read = true)))
        assertTrue(InboxFilter.ALL.matches(notif("TICKET_REPLY")))
    }

    @Test
    fun unread_matchesOnlyUnread() {
        assertTrue(InboxFilter.UNREAD.matches(notif("LEAVE_APPROVED", read = false)))
        assertFalse(InboxFilter.UNREAD.matches(notif("LEAVE_APPROVED", read = true)))
    }

    @Test
    fun approvals_matchesRequestKindsOnly() {
        assertTrue(InboxFilter.APPROVALS.matches(notif("LEAVE_REQUESTED")))
        assertTrue(InboxFilter.APPROVALS.matches(notif("WFH_REQUESTED")))
        assertTrue(InboxFilter.APPROVALS.matches(notif("USER_APPROVAL")))
        assertTrue(InboxFilter.APPROVALS.matches(notif("ONBOARDING_SUBMITTED")))
        assertFalse(InboxFilter.APPROVALS.matches(notif("LEAVE_APPROVED")))
        assertFalse(InboxFilter.APPROVALS.matches(notif("TICKET_REPLY")))
    }

    @Test
    fun tickets_matchesTicketKinds() {
        assertTrue(InboxFilter.TICKETS.matches(notif("TICKET_REPLY")))
        assertTrue(InboxFilter.TICKETS.matches(notif("TICKET_STATUS")))
        assertFalse(InboxFilter.TICKETS.matches(notif("LEAVE_APPROVED")))
    }

    @Test
    fun isApprovalRequest_matchesIosContract() {
        assertTrue(NotificationKind.LEAVE_REQUESTED.isApprovalRequest)
        assertTrue(NotificationKind.LEAVE_CANCELLATION_REQUESTED.isApprovalRequest)
        assertTrue(NotificationKind.WFH_REQUESTED.isApprovalRequest)
        assertTrue(NotificationKind.USER_APPROVAL.isApprovalRequest)
        assertTrue(NotificationKind.ONBOARDING_SUBMITTED.isApprovalRequest)
        assertFalse(NotificationKind.LEAVE_APPROVED.isApprovalRequest)
        assertFalse(NotificationKind.TICKET_REPLY.isApprovalRequest)
        assertFalse(NotificationKind.OTHER.isApprovalRequest)
    }

    @Test
    fun compactTimestamp_todayShowsTimeOtherwiseMonthDay() {
        val today = notif("LEAVE_APPROVED") // created 2026-07-02T10:00Z
        val stamp = today.compactTimestamp(now = now, zone = ZoneOffset.UTC)
        assertEquals("10:00 AM", stamp)

        val older = AppNotification(id = 2, type = "LEAVE_APPROVED", title = "t", message = "m", createdAt = "2026-06-29T08:00:00Z")
        assertEquals("Jun 29", older.compactTimestamp(now = now, zone = ZoneOffset.UTC))
    }

    @Test
    fun emptyMessage_perFilter() {
        assertEquals("You're all caught up.", InboxFilter.ALL.emptyMessage)
        assertEquals("No approval requests right now.", InboxFilter.APPROVALS.emptyMessage)
        assertEquals("No ticket updates right now.", InboxFilter.TICKETS.emptyMessage)
    }
}
