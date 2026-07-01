package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/** A per-leave-type balance from `/leave-applications/balances/my`. Mirrors iOS `LeaveBalance`. */
@Serializable
data class LeaveBalance(
    val id: Int,
    val leaveTypeId: Int? = null,
    val leaveTypeName: String? = null,
    val year: Int? = null,
    val totalDays: Double? = null,
    val usedDays: Double? = null,
    val pendingDays: Double? = null,
    val remainingDays: Double? = null,
)
