package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/**
 * A leave application from `/leave-applications`. `@JsonInclude(NON_NULL)` on the backend means most
 * fields may be absent, so they are optional. [status] is always present. Mirrors iOS
 * `LeaveApplication`.
 */
@Serializable
data class LeaveApplication(
    val id: Int,
    val leaveTypeId: Int? = null,
    val leaveTypeName: String? = null,
    val leaveTypeColor: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val totalDays: Double? = null,
    val reason: String? = null,
    val status: String,
    val statusLabel: String? = null,
    /** The applicant's name; populated on approval-inbox responses (`pending-approvals`). */
    val userName: String? = null,
) {
    /** The action an employee may take on this application. */
    enum class Action { CANCEL, REQUEST_CANCELLATION, NONE }

    /** The action an employee may take, derived from [status]. */
    val availableAction: Action
        get() = when (status) {
            "PENDING_MANAGER", "PENDING_HR" -> Action.CANCEL
            "APPROVED" -> Action.REQUEST_CANCELLATION
            else -> Action.NONE
        }

    val displayStatus: String get() = statusLabel ?: status

    /** Which approval stage this pending application is at; null if not pending approval. */
    val approvalStage: LeaveApprovalStage?
        get() = when (status) {
            "PENDING_MANAGER" -> LeaveApprovalStage.MANAGER
            "PENDING_HR" -> LeaveApprovalStage.HR
            else -> null
        }

    /**
     * Editable only while awaiting the manager — the backend rejects edits in any other state, and
     * the leave *type* can't change. Drives the Edit affordance on the detail screen.
     */
    val isEditable: Boolean get() = status == "PENDING_MANAGER"

    /**
     * A request to cancel an already-approved leave, awaiting HR/admin review. Such items appear in
     * the approver inbox and are actioned via the dedicated cancellation endpoints.
     */
    val isCancellationRequest: Boolean get() = status == "PENDING_CANCELLATION"
}
