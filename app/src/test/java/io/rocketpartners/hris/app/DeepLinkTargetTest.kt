package io.rocketpartners.hris.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkTargetTest {

    @Test
    fun fromData_prefersReferenceTypeAndId() {
        val target = DeepLinkTarget.fromData(mapOf("referenceType" to "LEAVE_REQUESTED", "referenceId" to "42"))
        assertEquals(DeepLinkTarget.Notification("LEAVE_REQUESTED", 42), target)
    }

    @Test
    fun fromData_fallsBackToType() {
        val target = DeepLinkTarget.fromData(mapOf("type" to "WFH_APPROVED"))
        assertEquals(DeepLinkTarget.Notification("WFH_APPROVED", null), target)
    }

    @Test
    fun fromData_referenceIdOnlyStillNotification() {
        val target = DeepLinkTarget.fromData(mapOf("referenceId" to "7"))
        assertEquals(DeepLinkTarget.Notification(null, 7), target)
    }

    @Test
    fun fromData_ignoresBlankAndUnparseableId() {
        val target = DeepLinkTarget.fromData(mapOf("referenceType" to "  ", "referenceId" to "abc"))
        assertNull(target)
    }

    @Test
    fun fromData_emptyReturnsNull() {
        assertNull(DeepLinkTarget.fromData(emptyMap()))
    }

    @Test
    fun fromUri_tabWithSegment() {
        val target = DeepLinkTarget.fromUri("hris", "tab", "calendar", emptyMap())
        assertEquals(DeepLinkTarget.Tab("calendar"), target)
    }

    @Test
    fun fromUri_applyLeaveAndScheduleWfh() {
        assertTrue(DeepLinkTarget.fromUri("hris", "apply-leave", null, emptyMap()) is DeepLinkTarget.ApplyLeave)
        assertTrue(DeepLinkTarget.fromUri("hris", "schedule-wfh", null, emptyMap()) is DeepLinkTarget.ScheduleWfh)
    }

    @Test
    fun fromUri_notificationQuery() {
        val target = DeepLinkTarget.fromUri("hris", "notification", null, mapOf("referenceType" to "TICKET_REPLY", "referenceId" to "9"))
        assertEquals(DeepLinkTarget.Notification("TICKET_REPLY", 9), target)
    }

    @Test
    fun fromUri_rejectsWrongScheme() {
        assertNull(DeepLinkTarget.fromUri("https", "tab", "home", emptyMap()))
    }

    @Test
    fun fromUri_unknownHostReturnsNull() {
        assertNull(DeepLinkTarget.fromUri("hris", "wat", null, emptyMap()))
    }
}
