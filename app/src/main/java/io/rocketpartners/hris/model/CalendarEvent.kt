package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A calendar event from `GET /calendar/events`. [start]/[end] are wire strings (date-only or ISO
 * datetime); [startDate]/[endDate] parse them lazily. Defensive optionals. Mirrors iOS.
 */
@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val start: String? = null,
    val end: String? = null,
    val allDay: Boolean = false,
    val color: String? = null,
    val type: String? = null,
) {
    val startDate: Instant? get() = start?.let(WireDate::parse)
    val endDate: Instant? get() = end?.let(WireDate::parse)
}
