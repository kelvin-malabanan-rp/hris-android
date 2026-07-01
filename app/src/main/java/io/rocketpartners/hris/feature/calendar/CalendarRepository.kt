package io.rocketpartners.hris.feature.calendar

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.CalendarEvent
import io.rocketpartners.hris.model.UserOnLeave
import java.time.LocalDate

/** Calendar data. `LocalDate.toString()` is already the `yyyy-MM-dd` wire format. Mirrors iOS. */
interface CalendarRepository {
    /** Events for an inclusive `[start, end]` day range. */
    suspend fun events(start: LocalDate, end: LocalDate): List<CalendarEvent>

    /** Approved leaves covering [date]. */
    suspend fun usersOnLeave(date: LocalDate): List<UserOnLeave>
}

class LiveCalendarRepository(private val client: ApiClient) : CalendarRepository {
    override suspend fun events(start: LocalDate, end: LocalDate): List<CalendarEvent> =
        client.send(
            Endpoint(
                "calendar/events",
                query = listOf("start" to start.toString(), "end" to end.toString()),
            ),
        )

    override suspend fun usersOnLeave(date: LocalDate): List<UserOnLeave> =
        client.send(Endpoint("calendar/users-on-leave", query = listOf("date" to date.toString())))
}
