package io.rocketpartners.hris.feature.calendar

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.model.CalendarEvent
import io.rocketpartners.hris.support.FakeCalendarRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarStoreTest {

    private val today = LocalDate.of(2026, 6, 15)

    private val events = listOf(
        CalendarEvent(id = "1", title = "Sprint", start = "2026-06-15T09:00:00Z", type = "meeting", color = "#0A84FF"),
        CalendarEvent(id = "2", title = "Offsite", start = "2026-06-24", end = "2026-06-25", allDay = true, type = "event"),
        CalendarEvent(id = "3", title = "Holiday", start = "2026-06-12", allDay = true, type = "holiday"),
    )

    @Test
    fun eventsOn_matchesSingleAndMultiDayByCivilDay() {
        val state = CalendarUiState(events = events, visibleMonth = YearMonth.from(today), selectedDate = today)
        assertEquals(listOf("1"), state.eventsOn(LocalDate.of(2026, 6, 15)).map { it.id })
        // Multi-day offsite spans the 24th and 25th.
        assertEquals(listOf("2"), state.eventsOn(LocalDate.of(2026, 6, 25)).map { it.id })
        assertTrue(state.eventsOn(LocalDate.of(2026, 6, 26)).isEmpty())
    }

    @Test
    fun presentTypes_areDistinctInFirstAppearanceOrder() {
        val state = CalendarUiState(events = events, visibleMonth = YearMonth.from(today), selectedDate = today)
        assertEquals(listOf("meeting", "event", "holiday"), state.presentTypes.map { it.slug })
        assertEquals("Meeting", state.presentTypes.first().label)
    }

    @Test
    fun filteredEvents_appliesActiveTypeFilter() {
        val state = CalendarUiState(events = events, visibleMonth = YearMonth.from(today), selectedDate = today, activeTypes = setOf("holiday"))
        assertEquals(listOf("3"), state.filteredEvents.map { it.id })
    }

    @Test
    fun load_populatesEventsAndLeavesAndFlipsToLoaded() = runTest {
        val store = CalendarStore(FakeCalendarRepository(eventsResult = Result.success(events)), today)
        store.load()
        assertEquals(Phase.Loaded, store.state.value.phase)
        assertEquals(3, store.state.value.events.size)
    }

    @Test
    fun advanceMonth_resetsSelectionAndClearsData() {
        val store = CalendarStore(FakeCalendarRepository(eventsResult = Result.success(events)), today)
        store.advanceMonth(1)
        assertEquals(YearMonth.of(2026, 7), store.state.value.visibleMonth)
        assertEquals(LocalDate.of(2026, 7, 1), store.state.value.selectedDate)
        assertTrue(store.state.value.events.isEmpty())
    }

    @Test
    fun toggleType_addsThenRemoves() {
        val store = CalendarStore(FakeCalendarRepository(), today)
        store.toggleType("meeting")
        assertEquals(setOf("meeting"), store.state.value.activeTypes)
        store.toggleType("meeting")
        assertTrue(store.state.value.activeTypes.isEmpty())
    }
}
