package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A notification from `GET /notifications`. Wire field [createdAt] is an ISO/date string;
 * [createdAtDate] parses it lazily. Defensive — never trust the payload. Mirrors iOS.
 */
@Serializable
data class AppNotification(
    val id: Int,
    val type: String,
    val title: String,
    val message: String,
    val referenceType: String? = null,
    val referenceId: Int? = null,
    val isRead: Boolean = false,
    val readAt: String? = null,
    val createdAt: String,
) {
    val createdAtDate: Instant? get() = WireDate.parse(createdAt)

    /** Lenient mapping of the backend [type] string to a known [NotificationKind]. */
    val kind: NotificationKind get() = NotificationKind.fromWire(type)

    /**
     * The tab this notification points at, derived from [referenceType] (falling back to [type]).
     * `null` when there's no dedicated in-app destination yet (tickets, onboarding) — callers should
     * leave the user where they are rather than route nowhere.
     */
    val routedTab: String?
        get() {
            val value = (referenceType ?: type).uppercase()
            return when {
                value.contains("LEAVE") -> "leave"
                value.contains("WFH") -> "wfh"
                value.contains("CALENDAR") || value.contains("EVENT") -> "calendar"
                else -> null
            }
        }
}

/**
 * Known notification categories. Unknown wire types fall back to [OTHER]. Icon/color mapping lives
 * in the notifications feature (platform-specific), not here. Mirrors iOS `NotificationKind`.
 */
enum class NotificationKind {
    TICKET_REPLY,
    TICKET_STATUS,
    LEAVE_REQUESTED,
    LEAVE_APPROVED,
    LEAVE_REJECTED,
    LEAVE_CANCELLED,
    LEAVE_CANCELLATION_REQUESTED,
    LEAVE_CANCELLATION_APPROVED,
    LEAVE_CANCELLATION_REJECTED,
    WFH_REQUESTED,
    WFH_APPROVED,
    WFH_REJECTED,
    USER_APPROVAL,
    ONBOARDING_SUBMITTED,
    ONBOARDING_APPROVED,
    ONBOARDING_REJECTED,
    ONBOARDING_UPDATE,
    OTHER;

    companion object {
        fun fromWire(wire: String): NotificationKind = when (wire.uppercase()) {
            "TICKET_REPLY" -> TICKET_REPLY
            "TICKET_STATUS" -> TICKET_STATUS
            "LEAVE_REQUESTED" -> LEAVE_REQUESTED
            "LEAVE_APPROVED", "LEAVE_APPROVED_BY_MANAGER" -> LEAVE_APPROVED
            "LEAVE_REJECTED", "LEAVE_REJECTED_BY_MANAGER" -> LEAVE_REJECTED
            "LEAVE_CANCELLED" -> LEAVE_CANCELLED
            "LEAVE_CANCELLATION_REQUESTED" -> LEAVE_CANCELLATION_REQUESTED
            "LEAVE_CANCELLATION_APPROVED" -> LEAVE_CANCELLATION_APPROVED
            "LEAVE_CANCELLATION_REJECTED" -> LEAVE_CANCELLATION_REJECTED
            "WFH_REQUESTED" -> WFH_REQUESTED
            "WFH_APPROVED_BY_MANAGER", "WFH_APPROVED" -> WFH_APPROVED
            "WFH_REJECTED_BY_MANAGER", "WFH_REJECTED" -> WFH_REJECTED
            "USER_APPROVAL" -> USER_APPROVAL
            "ONBOARDING_SUBMITTED" -> ONBOARDING_SUBMITTED
            "ONBOARDING_APPROVED" -> ONBOARDING_APPROVED
            "ONBOARDING_REJECTED" -> ONBOARDING_REJECTED
            "ONBOARDING_UPDATE" -> ONBOARDING_UPDATE
            else -> OTHER
        }
    }
}
