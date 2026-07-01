package io.rocketpartners.hris.feature.calendar

import io.rocketpartners.hris.core.networking.WireDate
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.CalendarEvent
import io.rocketpartners.hris.model.UserOnLeave
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One distinct event type present in the month, for the filter chips row. Mirrors iOS. */
data class EventTypeChip(val slug: String, val label: String, val color: String?)

data class CalendarUiState(
    val phase: Phase = Phase.Idle,
    val events: List<CalendarEvent> = emptyList(),
    val usersOnLeave: List<UserOnLeave> = emptyList(),
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val activeTypes: Set<String> = emptySet(),
) {
    /** Month events after applying the active type filter (empty filter = all). */
    val filteredEvents: List<CalendarEvent>
        get() = if (activeTypes.isEmpty()) events else events.filter { it.type in activeTypes }

    /** Distinct event types in the unfiltered month, in first-appearance order. */
    val presentTypes: List<EventTypeChip>
        get() {
            val seen = LinkedHashMap<String, EventTypeChip>()
            for (event in events) {
                val slug = event.type ?: continue
                if (slug !in seen) {
                    seen[slug] = EventTypeChip(slug, slug.replaceFirstChar(Char::uppercase), event.color)
                }
            }
            return seen.values.toList()
        }

    /**
     * Filtered events whose civil `[start, end]` day range covers [day], inclusive and multi-day
     * aware. Event date-only values parse at UTC, so both sides reduce to UTC civil days. Mirrors
     * iOS `CalendarStore.events(on:)`.
     */
    fun eventsOn(day: LocalDate): List<CalendarEvent> = filteredEvents.filter { event ->
        val start = event.startDate ?: return@filter false
        val startDay = WireDate.civilDay(start)
        val endDay = event.endDate?.let(WireDate::civilDay) ?: startDay
        !day.isBefore(startDay) && !day.isAfter(endDay)
    }
}

/**
 * Drives the Calendar screen: month events + per-day people-on-leave, with optimistic month paging
 * and staleness guards so rapid navigation never shows another month's data. Mirrors iOS
 * `CalendarStore`. Suspend methods are driven by the caller's coroutine scope.
 */
class CalendarStore(
    private val repository: CalendarRepository,
    today: LocalDate = LocalDate.now(),
) {
    private val _state = MutableStateFlow(
        CalendarUiState(visibleMonth = YearMonth.from(today), selectedDate = today),
    )
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    suspend fun load() {
        loadMonth()
        loadLeaves(_state.value.selectedDate)
    }

    /** Advance the visible month synchronously (no I/O), resetting selection + clearing stale data. */
    fun advanceMonth(months: Long) {
        _state.update {
            val month = it.visibleMonth.plusMonths(months)
            it.copy(visibleMonth = month, selectedDate = month.atDay(1), events = emptyList(), usersOnLeave = emptyList())
        }
    }

    fun goToToday() {
        val today = LocalDate.now()
        _state.update {
            it.copy(visibleMonth = YearMonth.from(today), selectedDate = today, events = emptyList(), usersOnLeave = emptyList())
        }
    }

    suspend fun reloadVisibleMonth() {
        loadMonth()
        loadLeaves(_state.value.selectedDate)
    }

    suspend fun showNextMonth() {
        advanceMonth(1)
        reloadVisibleMonth()
    }

    suspend fun showPreviousMonth() {
        advanceMonth(-1)
        reloadVisibleMonth()
    }

    suspend fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        loadLeaves(date)
    }

    fun toggleType(slug: String) {
        _state.update {
            it.copy(activeTypes = if (slug in it.activeTypes) it.activeTypes - slug else it.activeTypes + slug)
        }
    }

    fun clearTypes() {
        _state.update { it.copy(activeTypes = emptySet()) }
    }

    private suspend fun loadMonth() {
        val month = _state.value.visibleMonth
        if (_state.value.phase != Phase.Loaded) _state.update { it.copy(phase = Phase.Loading) }
        try {
            val fetched = repository.events(month.atDay(1), month.atEndOfMonth())
            if (_state.value.visibleMonth != month) return
            _state.update { it.copy(events = fetched, phase = Phase.Loaded) }
        } catch (e: Exception) {
            if (_state.value.visibleMonth != month) return
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load events."))) }
        }
    }

    private suspend fun loadLeaves(date: LocalDate) {
        try {
            val fetched = repository.usersOnLeave(date)
            if (_state.value.selectedDate != date) return
            _state.update { it.copy(usersOnLeave = fetched) }
        } catch (_: Exception) {
            if (_state.value.selectedDate != date) return
            _state.update { it.copy(usersOnLeave = emptyList()) }
        }
    }
}
