package io.rocketpartners.hris.feature.home

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.UserProfile
import io.rocketpartners.hris.model.WfhWeeklyUsage
import io.rocketpartners.hris.support.FakeAnnouncementRepository
import io.rocketpartners.hris.support.FakeCalendarRepository
import io.rocketpartners.hris.support.FakeLeaveRepository
import io.rocketpartners.hris.support.FakeProfileRepository
import io.rocketpartners.hris.support.FakeWfhRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStoreTest {

    private fun store(
        leave: FakeLeaveRepository = FakeLeaveRepository(),
        wfh: FakeWfhRepository = FakeWfhRepository(),
        calendar: FakeCalendarRepository = FakeCalendarRepository(),
        profile: FakeProfileRepository = FakeProfileRepository(),
        announcements: FakeAnnouncementRepository = FakeAnnouncementRepository(),
    ) = HomeStore(leave, wfh, calendar, profile, announcements, today = LocalDate.of(2026, 6, 15))

    @Test
    fun load_populatesSectionsFromRepositories() = runTest {
        val leave = FakeLeaveRepository(
            myLeavesResult = Result.success(
                listOf(
                    LeaveApplication(id = 1, startDate = "2026-06-22", status = "PENDING_MANAGER"),
                    LeaveApplication(id = 2, startDate = "2026-06-05", status = "APPROVED"),
                ),
            ),
            balancesResult = Result.success(listOf(LeaveBalance(id = 1, remainingDays = 8.0), LeaveBalance(id = 2, remainingDays = 9.0))),
        )
        val wfh = FakeWfhRepository(weeklyUsageResult = Result.success(WfhWeeklyUsage(2, 3, 1)))
        val profile = FakeProfileRepository(Result.success(UserProfile(id = 42, firstName = "Angelo")))
        val s = store(leave = leave, wfh = wfh, profile = profile)

        s.load()

        val state = s.state.value
        assertEquals(Phase.Loaded, state.phase)
        assertEquals(17.0, state.leaveBalanceRemaining!!, 0.0001)
        assertEquals(1, state.pendingCount)
        assertEquals(2, state.weeklyUsage!!.used)
        assertEquals("Angelo", state.profile!!.firstName)
    }

    @Test
    fun aggregatePhase_failsOnlyWhenEverySectionFails() = runTest {
        val err = Result.failure<Nothing>(ApiError.Network("offline"))
        val s = store(
            leave = FakeLeaveRepository(myLeavesResult = err, balancesResult = err),
            wfh = FakeWfhRepository(weeklyUsageResult = err),
            calendar = FakeCalendarRepository(usersOnLeaveResult = err),
            profile = FakeProfileRepository(err),
            announcements = FakeAnnouncementRepository(feedResult = err),
        )
        s.load()
        assertEquals(Phase.Failed::class, s.state.value.phase::class)
    }

    @Test
    fun recent_ordersByStartDateDescendingCappedAtThree() {
        val leaves = listOf(
            LeaveApplication(id = 1, startDate = "2026-01-01", status = "APPROVED"),
            LeaveApplication(id = 2, startDate = "2026-06-01", status = "APPROVED"),
            LeaveApplication(id = 3, startDate = "2026-03-01", status = "APPROVED"),
            LeaveApplication(id = 4, startDate = "2026-05-01", status = "APPROVED"),
        )
        // Exercised through load() so the private helper's effect is observable.
        val s = store(leave = FakeLeaveRepository(myLeavesResult = Result.success(leaves)))
        kotlinx.coroutines.test.runTest { s.load() }
        assertEquals(listOf(2, 4, 3), s.state.value.recentRequests.map { it.id })
    }
}
