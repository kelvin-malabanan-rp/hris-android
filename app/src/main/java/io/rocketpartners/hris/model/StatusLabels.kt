package io.rocketpartners.hris.model

/**
 * Turns a raw status token into a Title-Cased, space-separated label — e.g. `"pending"` → "Pending",
 * `"PENDING_MANAGER"` → "Pending Manager". Mirrors iOS `StatusBadge.humanLabel`; lives in the model
 * layer so both models (e.g. [Ticket.statusLabel]) and the design system can share it.
 */
fun humanStatusLabel(raw: String): String =
    raw.split('_', ' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar(Char::uppercase) }

/** Which approval stage a pending leave application is at. Mirrors iOS `LeaveApprovalStage`. */
enum class LeaveApprovalStage { MANAGER, HR }
