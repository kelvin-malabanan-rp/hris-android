package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/** An approved leave covering a queried date, from `GET /calendar/users-on-leave`. Mirrors iOS. */
@Serializable
data class UserOnLeave(
    val id: Int,
    val user: LeaveUser,
    val leaveType: LeaveTypeRef,
    val startDate: String,
    val endDate: String,
    val totalDays: Double,
)

@Serializable
data class LeaveUser(
    val id: Int,
    val name: String,
    val avatar: String? = null,
    val department: DepartmentRef? = null,
)

@Serializable
data class DepartmentRef(
    val id: Int,
    val name: String,
)

@Serializable
data class LeaveTypeRef(
    val id: Int,
    val name: String,
    val color: String,
)
