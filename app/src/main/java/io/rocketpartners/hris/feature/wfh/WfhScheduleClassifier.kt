package io.rocketpartners.hris.feature.wfh

import io.rocketpartners.hris.model.WfhSchedule
import java.time.LocalDate

/**
 * Outcome of submitting a batch of WFH days. [Success] carries the schedules the backend created —
 * one per *successfully processed* day (past/duplicate days are silently skipped), so it can be
 * shorter than the request. [Failure] carries a user-facing message. Mirrors iOS `WfhBatchResult`.
 */
sealed interface WfhBatchResult {
    data class Success(val created: List<WfhSchedule>) : WfhBatchResult
    data class Failure(val message: String) : WfhBatchResult
}

/**
 * Diffs a multi-day WFH request against the backend's response so the user sees a per-day outcome:
 * scheduled (within quota), sent-for-approval (over quota → manager), or skipped (past/duplicate).
 * This is the client-side view of the WFH quota-deduction logic. Mirrors iOS `WfhScheduleClassifier`.
 */
object WfhScheduleClassifier {
    data class Result(
        val scheduled: List<WfhSchedule> = emptyList(), // approved within quota (or a reactivated day)
        val pending: List<WfhSchedule> = emptyList(),   // over quota → routed to the manager
        val skipped: List<LocalDate> = emptyList(),     // requested but not returned: past or already active
    )

    /**
     * [requested] are the dates the user picked; [response] is what the backend created. Matching is
     * by the same `yyyy-MM-dd` wire key the request was sent with, so request/response align exactly.
     */
    fun classify(requested: List<LocalDate>, response: List<WfhSchedule>): Result {
        val returnedKeys = response.map { it.date }.toSet()
        val scheduled = response.filter { (it.status ?: "").lowercase() != "pending" }
        val pending = response.filter { (it.status ?: "").lowercase() == "pending" }
        val skipped = requested.filter { it.toString() !in returnedKeys }
        return Result(scheduled = scheduled, pending = pending, skipped = skipped)
    }
}
