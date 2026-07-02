package io.rocketpartners.hris.feature.timeoff

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.feature.leave.ApplyLeaveStore
import io.rocketpartners.hris.feature.leave.LeaveStore
import io.rocketpartners.hris.feature.wfh.WfhStore
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.LeaveType
import io.rocketpartners.hris.model.WfhSchedule
import io.rocketpartners.hris.support.FakeLeaveRepository
import io.rocketpartners.hris.support.FakeWfhRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeOffStoresTest {

    @Test
    fun merge_interleavesNewestDatedFirstUndatedLast() {
        val leaves = listOf(
            LeaveApplication(id = 1, startDate = "2026-07-01", status = "APPROVED"),
            LeaveApplication(id = 2, startDate = null, status = "APPROVED"),
        )
        val wfh = listOf(WfhSchedule(id = 5, date = "2026-07-10"))
        val merged = ScheduleRequest.merge(leaves, wfh)
        assertEquals(listOf("wfh-5", "leave-1", "leave-2"), merged.map { it.id })
    }

    @Test
    fun leaveStore_loadPopulatesLeavesAndBalances() = runTest {
        val repo = FakeLeaveRepository(
            myLeavesResult = Result.success(listOf(LeaveApplication(id = 1, status = "APPROVED"))),
            balancesResult = Result.success(listOf(LeaveBalance(id = 1, remainingDays = 5.0))),
        )
        val store = LeaveStore(repo)
        store.load()
        assertEquals(Phase.Loaded, store.state.value.phase)
        assertEquals(1, store.state.value.leaves.size)
        assertEquals(1, store.state.value.balances.size)
    }

    @Test
    fun leaveStore_loadFailsWhenLeavesThrow() = runTest {
        val store = LeaveStore(FakeLeaveRepository(myLeavesResult = Result.failure(ApiError.Network("x"))))
        store.load()
        assertTrue(store.state.value.phase is Phase.Failed)
    }

    @Test
    fun leaveStore_cancelReplacesInPlace() = runTest {
        val cancelled = LeaveApplication(id = 1, status = "CANCELLED")
        val repo = FakeLeaveRepository(
            myLeavesResult = Result.success(listOf(LeaveApplication(id = 1, status = "PENDING_MANAGER"))),
            mutationResult = Result.success(cancelled),
        )
        val store = LeaveStore(repo)
        store.load()
        store.cancel(store.state.value.leaves.first())
        assertEquals("CANCELLED", store.state.value.leaves.first().status)
    }

    @Test
    fun wfhStore_scheduleDayReloadsAndReturnsTrue() = runTest {
        val repo = FakeWfhRepository(schedulesResult = Result.success(listOf(WfhSchedule(id = 1, date = "2026-07-01"))))
        val store = WfhStore(repo)
        val ok = store.scheduleDay(LocalDate.of(2026, 7, 1), "focus")
        assertTrue(ok)
        assertEquals(1, store.state.value.schedules.size)
    }

    @Test
    fun applyLeaveStore_requestedDaysAndExceedsBalance() = runTest {
        val repo = FakeLeaveRepository(
            typesResult = Result.success(listOf(LeaveType(1, "Annual"))),
            balancesResult = Result.success(listOf(LeaveBalance(id = 1, leaveTypeId = 1, totalDays = 15.0, remainingDays = 2.0))),
        )
        val store = ApplyLeaveStore(repo, today = LocalDate.of(2026, 7, 1))
        store.loadTypes()
        store.setStart(LocalDate.of(2026, 7, 1))
        store.setEnd(LocalDate.of(2026, 7, 3))
        assertEquals(3, store.state.value.requestedDays)
        assertTrue(store.state.value.exceedsBalance)
        assertEquals(-1.0, store.state.value.projectedRemaining!!, 0.0001)
    }

    @Test
    fun applyLeaveStore_medicalCertAdvisoryTriggersOverThreshold() = runTest {
        val repo = FakeLeaveRepository(
            typesResult = Result.success(listOf(LeaveType(2, "Sick", requiresMedicalCert = true, medicalCertDaysThreshold = 2))),
        )
        val store = ApplyLeaveStore(repo, today = LocalDate.of(2026, 7, 1))
        store.loadTypes()
        store.setEnd(LocalDate.of(2026, 7, 4)) // 4 days > 2
        assertTrue(store.state.value.medicalCertAdvisory!!.contains("medical certificate"))
    }

    @Test
    fun applyLeaveStore_submitSuccess() = runTest {
        val repo = FakeLeaveRepository(
            typesResult = Result.success(listOf(LeaveType(1, "Annual"))),
            mutationResult = Result.success(LeaveApplication(id = 99, status = "PENDING_MANAGER")),
        )
        val store = ApplyLeaveStore(repo, today = LocalDate.of(2026, 7, 1))
        store.loadTypes()
        assertTrue(store.submit())
        assertEquals(99, store.savedApplication!!.id)
    }

    @Test
    fun applyLeaveStore_startAfterEndPushesEndForward() {
        val store = ApplyLeaveStore(FakeLeaveRepository(), today = LocalDate.of(2026, 7, 1))
        store.setEnd(LocalDate.of(2026, 7, 2))
        store.setStart(LocalDate.of(2026, 7, 5))
        assertFalse(store.state.value.endDate.isBefore(store.state.value.startDate))
    }
}
