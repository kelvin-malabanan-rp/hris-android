package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import kotlinx.serialization.Serializable

/** A WFH day from `/wfh/schedules`. Backend uses `@JsonInclude(NON_NULL)` → optionals. Mirrors iOS. */
@Serializable
data class WfhSchedule(
    val id: Int,
    val date: String,
    val dayName: String? = null,
    val type: String? = null,
    val reason: String? = null,
    val status: String? = null,
    // Populated on the manager pending-approvals path: who applied + the approver's note.
    val userId: Int? = null,
    val userName: String? = null,
    val managerComments: String? = null,
) {
    val parsedDate: Instant? get() = WireDate.parse(date)
}
