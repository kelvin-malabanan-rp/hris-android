package io.rocketpartners.hris.support

import io.rocketpartners.hris.feature.announcements.AnnouncementRepository
import io.rocketpartners.hris.feature.auth.AuthRepository
import io.rocketpartners.hris.feature.calendar.CalendarRepository
import io.rocketpartners.hris.feature.leave.LeaveRepository
import io.rocketpartners.hris.feature.leave.NewLeaveApplication
import io.rocketpartners.hris.feature.profile.ProfileRepository
import io.rocketpartners.hris.feature.profile.ProfileUpdate
import io.rocketpartners.hris.feature.wfh.WfhRepository
import io.rocketpartners.hris.model.Announcement
import io.rocketpartners.hris.model.CalendarEvent
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveApprovalStage
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.LeaveType
import io.rocketpartners.hris.model.User
import io.rocketpartners.hris.model.UserOnLeave
import io.rocketpartners.hris.model.UserProfile
import io.rocketpartners.hris.model.WfhSchedule
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.LocalDate
import java.time.YearMonth

/**
 * Hand-written fakes for store/service tests, mirroring the iOS `Tests/Support/Fakes.swift`
 * convention: `Result`-typed stub properties + recorded calls.
 */
class FakeAuthRepository(
    var loginResult: Result<User> = Result.success(User(1, "Ada", "ada@rp.io")),
    var currentUserResult: Result<User> = Result.success(User(1, "Ada", "ada@rp.io")),
    var logoutResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {
    val loginCalls = mutableListOf<Pair<String, String>>()
    var logoutCallCount = 0

    override suspend fun login(email: String, password: String): User {
        loginCalls.add(email to password)
        return loginResult.getOrThrow()
    }

    override suspend fun currentUser(): User = currentUserResult.getOrThrow()

    override suspend fun logout() {
        logoutCallCount++
        logoutResult.getOrThrow()
    }
}

class FakeCalendarRepository(
    var eventsResult: Result<List<CalendarEvent>> = Result.success(emptyList()),
    var usersOnLeaveResult: Result<List<UserOnLeave>> = Result.success(emptyList()),
) : CalendarRepository {
    override suspend fun events(start: LocalDate, end: LocalDate) = eventsResult.getOrThrow()
    override suspend fun usersOnLeave(date: LocalDate) = usersOnLeaveResult.getOrThrow()
}

class FakeLeaveRepository(
    var myLeavesResult: Result<List<LeaveApplication>> = Result.success(emptyList()),
    var balancesResult: Result<List<LeaveBalance>> = Result.success(emptyList()),
    var typesResult: Result<List<LeaveType>> = Result.success(emptyList()),
    var pendingApprovalsResult: Result<List<LeaveApplication>> = Result.success(emptyList()),
    var mutationResult: Result<LeaveApplication> = Result.success(LeaveApplication(id = 1, status = "PENDING_MANAGER")),
) : LeaveRepository {
    override suspend fun myLeaves() = myLeavesResult.getOrThrow()
    override suspend fun balances() = balancesResult.getOrThrow()
    override suspend fun activeLeaveTypes() = typesResult.getOrThrow()
    override suspend fun apply(draft: NewLeaveApplication) = mutationResult.getOrThrow()
    override suspend fun edit(id: Int, draft: NewLeaveApplication) = mutationResult.getOrThrow()
    override suspend fun cancel(id: Int) = mutationResult.getOrThrow()
    override suspend fun requestCancellation(id: Int, reason: String) = mutationResult.getOrThrow()
    override suspend fun pendingApprovals() = pendingApprovalsResult.getOrThrow()
    override suspend fun approve(id: Int, stage: LeaveApprovalStage, comments: String?) = mutationResult.getOrThrow()
    override suspend fun reject(id: Int, stage: LeaveApprovalStage, comments: String?) = mutationResult.getOrThrow()
    override suspend fun approveCancellation(id: Int, comments: String?) = mutationResult.getOrThrow()
    override suspend fun rejectCancellation(id: Int, comments: String?) = mutationResult.getOrThrow()
}

class FakeWfhRepository(
    var schedulesResult: Result<List<WfhSchedule>> = Result.success(emptyList()),
    var weeklyUsageResult: Result<WfhWeeklyUsage> = Result.success(WfhWeeklyUsage(0, 3, 3)),
    var pendingApprovalsResult: Result<List<WfhSchedule>> = Result.success(emptyList()),
    var mutationResult: Result<WfhSchedule> = Result.success(WfhSchedule(id = 1, date = "2026-07-01")),
) : WfhRepository {
    override suspend fun schedules(month: YearMonth) = schedulesResult.getOrThrow()
    override suspend fun weeklyUsage() = weeklyUsageResult.getOrThrow()
    override suspend fun schedule(dates: List<LocalDate>, reason: String?) = schedulesResult.getOrThrow()
    override suspend fun cancel(id: Int) = mutationResult.getOrThrow()
    override suspend fun pendingApprovals() = pendingApprovalsResult.getOrThrow()
    override suspend fun approve(id: Int, comments: String?) = mutationResult.getOrThrow()
    override suspend fun reject(id: Int, comments: String?) = mutationResult.getOrThrow()
}

class FakeProfileRepository(
    var profileResult: Result<UserProfile> = Result.success(UserProfile(id = 42, firstName = "Angelo")),
) : ProfileRepository {
    override suspend fun profile() = profileResult.getOrThrow()
    override suspend fun updateProfile(update: ProfileUpdate) = profileResult.getOrThrow()
    override suspend fun changePassword(current: String, new: String, confirm: String) = Unit
}

class FakeAnnouncementRepository(
    var feedResult: Result<List<Announcement>> = Result.success(emptyList()),
    var detailResult: Result<Announcement> = Result.success(Announcement(id = 1)),
    var imageResult: Result<ByteArray> = Result.success(ByteArray(0)),
) : AnnouncementRepository {
    override suspend fun feed(page: Int, category: String?, search: String?) = feedResult.getOrThrow()
    override suspend fun detail(id: Int) = detailResult.getOrThrow()
    override suspend fun imageData(path: String) = imageResult.getOrThrow()
}
