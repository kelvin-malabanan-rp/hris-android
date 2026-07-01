package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.AppJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSerializationTest {

    @Test
    fun user_composesNameFromFirstAndLast() {
        val u = AppJson.decodeFromString(
            User.serializer(),
            """{"id":1,"email":"a@b.co","firstName":"Ada","lastName":"Lovelace"}""",
        )
        assertEquals("Ada Lovelace", u.name)
        assertTrue(u.permissions.isEmpty())
    }

    @Test
    fun user_prefersDirectNameAndFallsBackToEmail() {
        val direct = AppJson.decodeFromString(
            User.serializer(),
            """{"id":1,"email":"a@b.co","name":"Grace Hopper","firstName":"X"}""",
        )
        assertEquals("Grace Hopper", direct.name)

        val emailFallback = AppJson.decodeFromString(
            User.serializer(),
            """{"id":2,"email":"only@b.co"}""",
        )
        assertEquals("only@b.co", emailFallback.name)
    }

    @Test
    fun user_permissionGatesReflectAuthorities() {
        val u = User(1, "n", "e", listOf("WFH_APPROVE_ALL", "LEAVE_APPLICATION_APPROVE"))
        assertTrue(u.canApproveWfh)
        assertTrue(u.canApproveLeave)
        assertFalse(User(1, "n", "e").canApproveWfh)
    }

    @Test
    fun announcement_pinnedDefaultsFalse_andCategoryLabelHumanized() {
        val a = AppJson.decodeFromString(
            Announcement.serializer(),
            """{"id":5,"category":"COMPANY_NEWS"}""",
        )
        assertFalse(a.pinned)
        assertEquals("Company News", a.categoryLabel)
    }

    @Test
    fun announcement_sortedImagePathsHonorSortOrderStably() {
        val a = Announcement(
            id = 1,
            images = listOf(
                AnnouncementImage(id = 1, url = "b", sortOrder = 2),
                AnnouncementImage(id = 2, url = "a", sortOrder = 1),
                AnnouncementImage(id = 3, url = "c", sortOrder = null),
                AnnouncementImage(id = 4, url = null, sortOrder = 0),
            ),
        )
        assertEquals(listOf("a", "b", "c"), a.sortedImagePaths)
    }

    @Test
    fun announcementList_pinnedFirstIsStable() {
        val list = listOf(
            Announcement(id = 1, pinned = false),
            Announcement(id = 2, pinned = true),
            Announcement(id = 3, pinned = false),
            Announcement(id = 4, pinned = true),
        )
        assertEquals(listOf(2, 4, 1, 3), list.pinnedFirst().map { it.id })
    }

    @Test
    fun leaveApplication_actionAndStageDerivedFromStatus() {
        assertEquals(
            LeaveApplication.Action.CANCEL,
            LeaveApplication(id = 1, status = "PENDING_MANAGER").availableAction,
        )
        assertEquals(
            LeaveApplication.Action.REQUEST_CANCELLATION,
            LeaveApplication(id = 1, status = "APPROVED").availableAction,
        )
        assertEquals(
            LeaveApprovalStage.HR,
            LeaveApplication(id = 1, status = "PENDING_HR").approvalStage,
        )
        assertTrue(LeaveApplication(id = 1, status = "PENDING_MANAGER").isEditable)
        assertNull(LeaveApplication(id = 1, status = "APPROVED").approvalStage)
    }

    @Test
    fun wfhWeeklyUsage_pendingDefaultsWhenAbsentOrNull() {
        val absent = AppJson.decodeFromString(
            WfhWeeklyUsage.serializer(),
            """{"used":1,"quota":2,"remaining":1}""",
        )
        assertEquals(0, absent.pending)
        val explicitNull = AppJson.decodeFromString(
            WfhWeeklyUsage.serializer(),
            """{"used":1,"quota":2,"remaining":1,"pending":null}""",
        )
        assertEquals(0, explicitNull.pending)
    }

    @Test
    fun leaveType_medicalCertAdvisoryThreshold() {
        val always = LeaveType(1, "Sick", requiresMedicalCert = true)
        assertTrue(always.requiresMedicalCertificate(1.0))
        val above = LeaveType(1, "Sick", requiresMedicalCert = true, medicalCertDaysThreshold = 2)
        assertFalse(above.requiresMedicalCertificate(2.0))
        assertTrue(above.requiresMedicalCertificate(3.0))
        assertFalse(LeaveType(1, "Vacation").requiresMedicalCertificate(10.0))
    }

    @Test
    fun ticketDetail_decodesFlattenedTicketFieldsAndThread() {
        val json = """
            {"id":7,"subject":"Broken","status":"in_progress",
             "messages":[{"id":1,"isSupport":true,"message":"hi"}],
             "attachments":[{"id":9,"contentType":"image/png","fileSize":2048}]}
        """.trimIndent()
        val detail = AppJson.decodeFromString(TicketDetail.serializer(), json)
        assertEquals(7, detail.id)
        assertEquals("In Progress", detail.ticket.statusLabel)
        assertEquals(1, detail.messages.size)
        assertTrue(detail.messages.first().isSupport)
        assertTrue(detail.attachments.first().isImage)
        assertEquals("2 KB", detail.attachments.first().formattedSize)
    }

    @Test
    fun ticket_isResolvedFromStatusOrResolvedAt() {
        assertTrue(Ticket(id = 1, status = "closed").isResolved)
        assertTrue(Ticket(id = 1, resolvedAt = "2026-01-01").isResolved)
        assertFalse(Ticket(id = 1, status = "open").isResolved)
    }
}
