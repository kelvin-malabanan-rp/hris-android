package io.rocketpartners.hris.feature.home

import io.rocketpartners.hris.core.networking.WireDate
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.feature.announcements.AnnouncementRepository
import io.rocketpartners.hris.feature.calendar.CalendarRepository
import io.rocketpartners.hris.feature.leave.LeaveRepository
import io.rocketpartners.hris.feature.profile.ProfileRepository
import io.rocketpartners.hris.feature.wfh.WfhRepository
import io.rocketpartners.hris.model.Announcement
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.UserOnLeave
import io.rocketpartners.hris.model.UserProfile
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The dashboard's state: five sections that each load concurrently and flip their own phase, so one
 * slow/failed call never blanks the whole screen. Mirrors iOS `HomeStore`.
 */
data class HomeUiState(
    val profilePhase: Phase = Phase.Idle,
    val leavePhase: Phase = Phase.Idle,
    val wfhPhase: Phase = Phase.Idle,
    val calendarPhase: Phase = Phase.Idle,
    val announcementsPhase: Phase = Phase.Idle,
    val profile: UserProfile? = null,
    val leaveBalanceRemaining: Double? = null,
    val pendingCount: Int = 0,
    val weeklyUsage: WfhWeeklyUsage? = null,
    val outToday: List<UserOnLeave> = emptyList(),
    val recentRequests: List<LeaveApplication> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
) {
    private val sections get() = listOf(profilePhase, leavePhase, wfhPhase, calendarPhase, announcementsPhase)

    /** `.Loaded` once any section succeeds, `.Failed` only when all fail, else `.Loading`/`.Idle`. */
    val phase: Phase
        get() {
            if (sections.any { it is Phase.Loaded }) return Phase.Loaded
            val failures = sections.filterIsInstance<Phase.Failed>()
            if (failures.size == sections.size) return failures.first()
            if (sections.any { it is Phase.Loading }) return Phase.Loading
            return Phase.Idle
        }
}

class HomeStore(
    private val leave: LeaveRepository,
    private val wfh: WfhRepository,
    private val calendar: CalendarRepository,
    private val profile: ProfileRepository,
    private val announcements: AnnouncementRepository,
    private val today: LocalDate = LocalDate.now(),
) {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Loads all sections concurrently; already-loaded sections stay loaded (no skeleton flash). */
    suspend fun load() = coroutineScope {
        _state.update {
            it.copy(
                profilePhase = keepLoaded(it.profilePhase),
                leavePhase = keepLoaded(it.leavePhase),
                wfhPhase = keepLoaded(it.wfhPhase),
                calendarPhase = keepLoaded(it.calendarPhase),
                announcementsPhase = keepLoaded(it.announcementsPhase),
            )
        }
        launch { loadProfile() }
        launch { loadLeave() }
        launch { loadWfh() }
        launch { loadCalendar() }
        launch { loadAnnouncements() }
    }

    private suspend fun loadProfile() {
        try {
            val p = profile.profile()
            _state.update { it.copy(profile = p, profilePhase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(profilePhase = Phase.Failed(errorMessage(e))) }
        }
    }

    private suspend fun loadLeave() {
        try {
            val balances = leave.balances()
            val mine = leave.myLeaves()
            _state.update {
                it.copy(
                    leaveBalanceRemaining = if (balances.isEmpty()) null else balances.sumOf { b -> b.remainingDays ?: 0.0 },
                    pendingCount = mine.count { l -> l.status.uppercase().contains("PENDING") },
                    recentRequests = recent(mine),
                    leavePhase = Phase.Loaded,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(leavePhase = Phase.Failed(errorMessage(e))) }
        }
    }

    private suspend fun loadWfh() {
        try {
            val usage = wfh.weeklyUsage()
            _state.update { it.copy(weeklyUsage = usage, wfhPhase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(wfhPhase = Phase.Failed(errorMessage(e))) }
        }
    }

    private suspend fun loadCalendar() {
        try {
            val out = calendar.usersOnLeave(today)
            _state.update { it.copy(outToday = out, calendarPhase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(calendarPhase = Phase.Failed(errorMessage(e))) }
        }
    }

    private suspend fun loadAnnouncements() {
        try {
            val feed = announcements.feed(page = 0, category = null, search = null)
            _state.update { it.copy(announcements = feed.take(3), announcementsPhase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(announcementsPhase = Phase.Failed(errorMessage(e))) }
        }
    }

    private companion object {
        fun keepLoaded(phase: Phase): Phase = if (phase is Phase.Loaded) phase else Phase.Loading

        /** Most-recent leaves first (by start date), capped at [limit]. */
        fun recent(leaves: List<LeaveApplication>, limit: Int = 3): List<LeaveApplication> =
            leaves.sortedByDescending { it.startDate?.let(WireDate::parse) ?: Instant.MIN }.take(limit)
    }
}
