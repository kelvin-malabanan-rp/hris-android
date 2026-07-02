package io.rocketpartners.hris.app

/**
 * A resolved deep-link destination. Mirrors the iOS `DeepLinkTarget` enum: a raw tab key, a
 * notification reference (which the tab host maps to a ticket / approvals inbox / tab), or the two
 * quick-action sheets. Parsed from a push payload's data map or an `hris://` URI.
 */
sealed interface DeepLinkTarget {
    data class Tab(val key: String) : DeepLinkTarget
    data class Notification(val referenceType: String?, val referenceId: Int?) : DeepLinkTarget
    data object ApplyLeave : DeepLinkTarget
    data object ScheduleWfh : DeepLinkTarget

    companion object {
        /**
         * Maps a push payload's data map to a target. Mirrors iOS `AppDelegate.deepLink(from:)`:
         * prefer `referenceType`/`referenceId`, fall back to a bare `type`, else nothing.
         */
        fun fromData(data: Map<String, String?>): DeepLinkTarget? {
            val referenceType = data["referenceType"]?.takeIf { it.isNotBlank() }
            val referenceId = data["referenceId"]?.toIntOrNull()
            if (referenceType != null || referenceId != null) {
                return Notification(referenceType, referenceId)
            }
            val type = data["type"]?.takeIf { it.isNotBlank() }
            if (type != null) return Notification(type, null)
            return null
        }

        /**
         * Parses an `hris://` deep-link URI. Supported forms:
         * `hris://tab/<key>`, `hris://apply-leave`, `hris://schedule-wfh`,
         * `hris://notification?referenceType=<t>&referenceId=<id>`.
         */
        fun fromUri(scheme: String?, host: String?, firstPathSegment: String?, query: Map<String, String?>): DeepLinkTarget? {
            if (scheme != "hris") return null
            return when (host) {
                "tab" -> firstPathSegment?.takeIf { it.isNotBlank() }?.let { Tab(it) }
                "apply-leave" -> ApplyLeave
                "schedule-wfh" -> ScheduleWfh
                "notification" -> Notification(
                    referenceType = query["referenceType"]?.takeIf { it.isNotBlank() },
                    referenceId = query["referenceId"]?.toIntOrNull(),
                )
                else -> null
            }
        }
    }
}
