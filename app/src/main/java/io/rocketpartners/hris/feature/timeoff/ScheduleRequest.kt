package io.rocketpartners.hris.feature.timeoff

import io.rocketpartners.hris.core.networking.WireDate
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.WfhSchedule
import java.time.Instant

/**
 * One row in the combined "My Requests" list — a leave application or a WFH day — so the two render
 * in a single date-sorted list with a type chip. Mirrors iOS `ScheduleRequest`.
 */
sealed interface ScheduleRequest {
    val id: String
    val kind: Kind
    val sortDate: Instant?

    enum class Kind { LEAVE, WFH }

    data class Leave(val leave: LeaveApplication) : ScheduleRequest {
        override val id = "leave-${leave.id}"
        override val kind = Kind.LEAVE
        override val sortDate: Instant? get() = leave.startDate?.let(WireDate::parse)
    }

    data class Wfh(val schedule: WfhSchedule) : ScheduleRequest {
        override val id = "wfh-${schedule.id}"
        override val kind = Kind.WFH
        override val sortDate: Instant? get() = schedule.parsedDate
    }

    companion object {
        /**
         * Merges leaves + WFH days into one list, newest-dated first. Undated entries sort last;
         * ties break on the stable [id] so ordering is deterministic. Mirrors iOS `merge`.
         */
        fun merge(leaves: List<LeaveApplication>, schedules: List<WfhSchedule>): List<ScheduleRequest> {
            val combined: List<ScheduleRequest> = leaves.map(::Leave) + schedules.map(::Wfh)
            return combined.sortedWith { lhs, rhs ->
                val l = lhs.sortDate
                val r = rhs.sortDate
                when {
                    l != null && r != null -> if (l == r) lhs.id.compareTo(rhs.id) else r.compareTo(l)
                    l != null -> -1
                    r != null -> 1
                    else -> lhs.id.compareTo(rhs.id)
                }
            }
        }
    }
}
