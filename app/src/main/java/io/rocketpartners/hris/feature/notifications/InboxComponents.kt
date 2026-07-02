package io.rocketpartners.hris.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.AppNotification
import io.rocketpartners.hris.model.NotificationKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// MARK: - Time grouping

/** One time bucket of the inbox ("Today", "Yesterday", …), preserving the store's ordering. */
data class InboxGroup(val title: String, val items: List<AppNotification>)

object InboxGrouping {
    private val ORDER = listOf("Today", "Yesterday", "Last 7 days", "Earlier")

    /**
     * Buckets notifications into Today / Yesterday / Last 7 days / Earlier by civil day. [now]/[zone]
     * are injectable for tests. Mirrors iOS `InboxGrouping.groups`.
     */
    fun groups(
        notifications: List<AppNotification>,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<InboxGroup> {
        val today = now.atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)
        val buckets = LinkedHashMap<String, MutableList<AppNotification>>()
        for (notification in notifications) {
            val instant = notification.createdAtDate
            val title = when {
                instant == null -> "Earlier"
                else -> {
                    val day = instant.atZone(zone).toLocalDate()
                    when {
                        // Clock skew can stamp a fresh notification on a later civil day; keep it in Today.
                        !day.isBefore(today) -> "Today"
                        day == yesterday -> "Yesterday"
                        ChronoUnit.DAYS.between(day, today) < 7 -> "Last 7 days"
                        else -> "Earlier"
                    }
                }
            }
            buckets.getOrPut(title) { mutableListOf() }.add(notification)
        }
        return ORDER.mapNotNull { title -> buckets[title]?.let { InboxGroup(title, it) } }
    }
}

// MARK: - Filters

/** Chip filters for the inbox. Pure so matching stays unit-testable. Mirrors iOS `InboxFilter`. */
enum class InboxFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    APPROVALS("Approvals"),
    TICKETS("Tickets");

    /** Empty-state copy when the active filter matches nothing. */
    val emptyMessage: String
        get() = when (this) {
            ALL, UNREAD -> "You're all caught up."
            APPROVALS -> "No approval requests right now."
            TICKETS -> "No ticket updates right now."
        }

    fun matches(notification: AppNotification): Boolean = when (this) {
        ALL -> true
        UNREAD -> !notification.isRead
        APPROVALS -> notification.kind.isApprovalRequest
        TICKETS -> notification.kind == NotificationKind.TICKET_REPLY || notification.kind == NotificationKind.TICKET_STATUS
    }
}

// MARK: - Model presentation helpers

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val MONTH_DAY_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * Compact trailing timestamp: time-of-day for today, otherwise "Jun 29". Sections already carry the
 * day context, so rows don't repeat relative phrases. Mirrors iOS `compactTimestamp`.
 */
fun AppNotification.compactTimestamp(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): String? {
    val instant = createdAtDate ?: return null
    val day = instant.atZone(zone)
    // Uppercase so the AM/PM marker matches iOS's "10:00 AM" (CLDR defaults to lowercase am/pm).
    return if (day.toLocalDate() == now.atZone(zone).toLocalDate()) day.format(TIME_FMT).uppercase(Locale.getDefault()) else day.format(MONTH_DAY_FMT)
}

/** Icon + semantic tint for a notification kind. Mirrors iOS `KindIcon` + `NotificationKind.tint`. */
data class KindVisual(val icon: ImageVector, val tint: Color)

@Composable
fun kindVisual(kind: NotificationKind): KindVisual {
    val green = Theme.statusColor("approved")
    val red = Theme.statusColor("rejected")
    val pending = Theme.statusColor("pending")
    // Icon per kind mirrors the iOS SF Symbol mapping (NotificationKind.systemImage); tint mirrors
    // NotificationKind.tint (approved→green, rejected→red, requests→pending, tickets/updates→brand).
    return when (kind) {
        NotificationKind.TICKET_REPLY -> KindVisual(Icons.AutoMirrored.Filled.Chat, Theme.brand)          // bubble.left.and.bubble.right
        NotificationKind.TICKET_STATUS -> KindVisual(Icons.Filled.ConfirmationNumber, Theme.brand)        // ticket
        NotificationKind.LEAVE_REQUESTED -> KindVisual(Icons.Filled.FlightTakeoff, pending)               // airplane.departure
        NotificationKind.LEAVE_APPROVED -> KindVisual(Icons.Filled.Verified, green)                       // checkmark.seal
        NotificationKind.LEAVE_REJECTED -> KindVisual(Icons.Filled.Cancel, red)                           // xmark.seal
        NotificationKind.LEAVE_CANCELLED -> KindVisual(Icons.Filled.Block, Theme.statusColor("cancelled")) // slash.circle
        NotificationKind.LEAVE_CANCELLATION_REQUESTED -> KindVisual(Icons.AutoMirrored.Filled.Undo, pending) // arrow.uturn.backward.circle
        NotificationKind.LEAVE_CANCELLATION_APPROVED -> KindVisual(Icons.Filled.CheckCircle, green)       // checkmark.circle
        NotificationKind.LEAVE_CANCELLATION_REJECTED -> KindVisual(Icons.Filled.Cancel, red)             // xmark.circle
        NotificationKind.WFH_REQUESTED -> KindVisual(Icons.Filled.Home, pending)                          // house.fill
        NotificationKind.WFH_APPROVED -> KindVisual(Icons.Filled.Home, green)                             // house.circle.fill
        NotificationKind.WFH_REJECTED -> KindVisual(Icons.Filled.Home, red)                               // house.slash
        NotificationKind.USER_APPROVAL -> KindVisual(Icons.Filled.VerifiedUser, green)                    // person.badge.shield.checkmark
        NotificationKind.ONBOARDING_SUBMITTED -> KindVisual(Icons.AutoMirrored.Filled.NoteAdd, pending)   // doc.badge.plus
        NotificationKind.ONBOARDING_APPROVED -> KindVisual(Icons.Filled.FactCheck, green)                 // doc.badge.checkmark
        NotificationKind.ONBOARDING_REJECTED -> KindVisual(Icons.Filled.Description, red)                 // doc.badge.ellipsis
        NotificationKind.ONBOARDING_UPDATE -> KindVisual(Icons.Filled.Description, Theme.brand)           // doc.text
        NotificationKind.OTHER -> KindVisual(Icons.Filled.Notifications, MaterialTheme.colorScheme.onSurfaceVariant) // bell
    }
}

// MARK: - Row pieces

/** Notification-kind glyph in a tinted circle; [size] scales it from list rows up to the detail hero. */
@Composable
fun KindIcon(kind: NotificationKind, size: Dp = Theme.Size.iconBadge, glyph: Dp = 20.dp) {
    val visual = kindVisual(kind)
    Box(Modifier.size(size).background(visual.tint.copy(alpha = Theme.Opacity.fill), CircleShape), contentAlignment = Alignment.Center) {
        Icon(visual.icon, contentDescription = null, tint = visual.tint, modifier = Modifier.size(glyph))
    }
}

/** A single inbox row: tinted icon, title + 2-line message, compact time + unread dot. */
@Composable
fun InboxRow(notification: AppNotification, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KindIcon(notification.kind)
        androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(notification.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold)
            Text(notification.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
            notification.compactTimestamp()?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (!notification.isRead) Box(Modifier.size(Theme.Size.unreadDot).background(Theme.brand, CircleShape))
        }
    }
}
