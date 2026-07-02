package io.rocketpartners.hris.feature.p5

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.feature.notifications.NotificationRepository
import io.rocketpartners.hris.feature.notifications.NotificationStore
import io.rocketpartners.hris.feature.search.GlobalSearchStore
import io.rocketpartners.hris.feature.tickets.TicketDetailStore
import io.rocketpartners.hris.feature.tickets.TicketFilters
import io.rocketpartners.hris.feature.tickets.TicketRepository
import io.rocketpartners.hris.feature.tickets.TicketsStore
import io.rocketpartners.hris.model.AppNotification
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.NewTicket
import io.rocketpartners.hris.model.Ticket
import io.rocketpartners.hris.model.TicketDetail
import io.rocketpartners.hris.model.TicketMessage
import io.rocketpartners.hris.model.UploadFile
import io.rocketpartners.hris.support.FakeCalendarRepository
import io.rocketpartners.hris.support.FakeLeaveRepository
import io.rocketpartners.hris.support.FakeWfhRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNotificationRepo(
    var listResult: Result<List<AppNotification>> = Result.success(emptyList()),
    var unread: Int = 0,
) : NotificationRepository {
    var markAllCount = 0
    override suspend fun list() = listResult.getOrThrow()
    override suspend fun unreadCount() = unread
    override suspend fun markRead(id: Int) = AppNotification(id, "LEAVE_APPROVED", "t", "m", isRead = true, createdAt = "2026-06-16T08:30:00Z")
    override suspend fun markAllRead() { markAllCount++ }
    override suspend fun registerDevice(token: String, environment: String) = Unit
    override suspend fun unregisterDevice(token: String) = Unit
}

private class FakeTicketRepo(
    var listResult: Result<List<Ticket>> = Result.success(emptyList()),
    var createResult: Result<Ticket> = Result.success(Ticket(id = 99, subject = "New")),
    var replyResult: Result<TicketMessage> = Result.success(TicketMessage(id = 5, message = "hi")),
    var detailResult: Result<TicketDetail> = Result.success(TicketDetail(Ticket(id = 1))),
) : TicketRepository {
    override suspend fun myTickets(filters: TicketFilters) = listResult.getOrThrow()
    override suspend fun ticket(id: Int) = detailResult.getOrThrow()
    override suspend fun create(draft: NewTicket, attachments: List<UploadFile>) = createResult.getOrThrow()
    override suspend fun reply(ticketId: Int, message: String, attachments: List<UploadFile>) = replyResult.getOrThrow()
    override suspend fun attachmentData(downloadUrl: String) = ByteArray(0)
}

class P5StoresTest {

    private fun notif(id: Int, read: Boolean) =
        AppNotification(id, "LEAVE_APPROVED", "Leave approved", "msg", isRead = read, createdAt = "2026-06-16T08:30:00Z")

    @Test
    fun notificationStore_loadUsesServerUnreadCount() = runTest {
        val store = NotificationStore(FakeNotificationRepo(listResult = Result.success(listOf(notif(1, false), notif(2, true))), unread = 1))
        store.load()
        assertEquals(Phase.Loaded, store.state.value.phase)
        assertEquals(1, store.state.value.unreadCount)
    }

    @Test
    fun notificationStore_markReadIsOptimistic() = runTest {
        val store = NotificationStore(FakeNotificationRepo(listResult = Result.success(listOf(notif(1, false))), unread = 1))
        store.load()
        store.markRead(store.state.value.notifications.first())
        assertTrue(store.state.value.notifications.first().isRead)
        assertEquals(0, store.state.value.unreadCount)
    }

    @Test
    fun notificationStore_markAllReadClearsUnread() = runTest {
        val repo = FakeNotificationRepo(listResult = Result.success(listOf(notif(1, false), notif(2, false))), unread = 2)
        val store = NotificationStore(repo)
        store.load()
        store.markAllRead()
        assertEquals(0, store.state.value.unreadCount)
        assertTrue(store.state.value.notifications.all { it.isRead })
        assertEquals(1, repo.markAllCount)
    }

    @Test
    fun ticketsStore_createPrependsOnSuccess() = runTest {
        val store = TicketsStore(FakeTicketRepo(listResult = Result.success(listOf(Ticket(id = 1))), createResult = Result.success(Ticket(id = 99, subject = "New"))))
        store.load()
        assertTrue(store.create(NewTicket(subject = "New", description = "d"), emptyList()))
        assertEquals(99, store.state.value.tickets.first().id)
    }

    @Test
    fun ticketDetailStore_replyAppendsMessage() = runTest {
        val store = TicketDetailStore(FakeTicketRepo(detailResult = Result.success(TicketDetail(Ticket(id = 1), messages = emptyList())), replyResult = Result.success(TicketMessage(id = 7, message = "reply"))))
        store.load(1)
        assertTrue(store.reply("reply", emptyList()))
        assertEquals(1, store.state.value.detail!!.messages.size)
        assertEquals(7, store.state.value.detail!!.messages.first().id)
    }

    @Test
    fun globalSearch_filtersByQueryAcrossSections() = runTest {
        val store = GlobalSearchStore(
            leaveRepo = FakeLeaveRepository(myLeavesResult = Result.success(listOf(LeaveApplication(id = 1, leaveTypeName = "Annual Leave", status = "APPROVED")))),
            wfhRepo = FakeWfhRepository(),
            calendarRepo = FakeCalendarRepository(),
            today = LocalDate.of(2026, 7, 2),
        )
        store.load()
        store.setQuery("annual")
        assertTrue(store.state.value.hasResults)
        assertEquals(1, store.state.value.matchedLeaves.size)
        store.setQuery("zzz")
        assertFalse(store.state.value.hasResults)
    }
}
