package io.rocketpartners.hris.feature.search

import io.rocketpartners.hris.feature.calendar.CalendarRepository
import io.rocketpartners.hris.feature.leave.LeaveRepository
import io.rocketpartners.hris.feature.wfh.WfhRepository
import io.rocketpartners.hris.model.CalendarEvent
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.UserOnLeave
import io.rocketpartners.hris.model.WfhSchedule
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GlobalSearchUiState(
    val query: String = "",
    val leaves: List<LeaveApplication> = emptyList(),
    val schedules: List<WfhSchedule> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val people: List<UserOnLeave> = emptyList(),
    val loaded: Boolean = false,
) {
    private val q: String get() = query.trim().lowercase()
    val hasQuery: Boolean get() = q.isNotEmpty()

    val matchedLeaves: List<LeaveApplication>
        get() = if (!hasQuery) emptyList() else leaves.filter { l ->
            listOfNotNull(l.leaveTypeName, l.reason, l.status, l.statusLabel).any { it.lowercase().contains(q) }
        }
    val matchedSchedules: List<WfhSchedule>
        get() = if (!hasQuery) emptyList() else schedules.filter { s ->
            listOfNotNull(s.date, s.dayName, s.status).any { it.lowercase().contains(q) } || "wfh work from home".contains(q)
        }
    val matchedEvents: List<CalendarEvent>
        get() = if (!hasQuery) emptyList() else events.filter { it.title.lowercase().contains(q) || (it.type?.lowercase()?.contains(q) ?: false) }
    val matchedPeople: List<UserOnLeave>
        get() = if (!hasQuery) emptyList() else people.filter { it.user.name.lowercase().contains(q) || it.leaveType.name.lowercase().contains(q) }

    val hasResults: Boolean
        get() = matchedLeaves.isNotEmpty() || matchedSchedules.isNotEmpty() || matchedEvents.isNotEmpty() || matchedPeople.isNotEmpty()
}

/** Loads leave/WFH/events/people once, then filters client-side by query. Mirrors iOS `GlobalSearchStore`. */
class GlobalSearchStore(
    private val leaveRepo: LeaveRepository,
    private val wfhRepo: WfhRepository,
    private val calendarRepo: CalendarRepository,
    private val today: LocalDate = LocalDate.now(),
) {
    private val _state = MutableStateFlow(GlobalSearchUiState())
    val state: StateFlow<GlobalSearchUiState> = _state.asStateFlow()

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    suspend fun load() {
        val leaves = runCatching { leaveRepo.myLeaves() }.getOrDefault(emptyList())
        val schedules = runCatching { wfhRepo.schedules(YearMonth.from(today)) }.getOrDefault(emptyList())
        val month = YearMonth.from(today)
        val events = runCatching { calendarRepo.events(month.atDay(1), month.atEndOfMonth()) }.getOrDefault(emptyList())
        val people = runCatching { calendarRepo.usersOnLeave(today) }.getOrDefault(emptyList())
        _state.update { it.copy(leaves = leaves, schedules = schedules, events = events, people = people, loaded = true) }
    }
}
