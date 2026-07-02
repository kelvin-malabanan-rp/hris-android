package io.rocketpartners.hris.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.AppNotification
import io.rocketpartners.hris.model.NotificationKind
import kotlinx.coroutines.launch

/** Notification inbox: per-kind icon/color, unread dot, tap-to-read, mark-all-read. Mirrors iOS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(repository: NotificationRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = remember { NotificationStore(repository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { store.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (state.unreadCount > 0) TextButton(onClick = { scope.launch { store.markAllRead() } }) { Text("Mark all read") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val phase = state.phase) {
                is Phase.Failed -> ErrorState(message = phase.message, retry = { store.load() })
                is Phase.Loaded -> if (state.notifications.isEmpty()) {
                    EmptyState(icon = Icons.Filled.NotificationsNone, title = "You're all caught up", message = "New notifications will appear here.", modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl))
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Theme.Spacing.sm)) {
                        state.notifications.forEach { n ->
                            NotificationRow(n) { scope.launch { store.markRead(n) } }
                        }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit) {
    val (icon, tint) = visual(n.kind)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(Theme.Size.iconBadge).background(tint.copy(alpha = Theme.Opacity.fill), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(n.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.SemiBold)
            Text(n.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!n.isRead) Box(Modifier.padding(top = 4.dp).size(Theme.Size.unreadDot).background(Theme.brand, CircleShape))
    }
}

/** Maps a notification kind to an icon + semantic color. Mirrors iOS `NotificationKind.systemImage`. */
@Composable
private fun visual(kind: NotificationKind): Pair<ImageVector, Color> {
    val green = Theme.statusColor("approved")
    val red = Theme.statusColor("rejected")
    val orange = Theme.Accent.PENDING.tint
    return when (kind) {
        NotificationKind.TICKET_REPLY -> Icons.AutoMirrored.Filled.Chat to Theme.brand
        NotificationKind.TICKET_STATUS -> Icons.Filled.Description to Theme.brand
        NotificationKind.LEAVE_REQUESTED -> Icons.Filled.FlightTakeoff to orange
        NotificationKind.LEAVE_APPROVED, NotificationKind.LEAVE_CANCELLATION_APPROVED -> Icons.Filled.CheckCircle to green
        NotificationKind.LEAVE_REJECTED, NotificationKind.LEAVE_CANCELLATION_REJECTED -> Icons.Filled.Cancel to red
        NotificationKind.LEAVE_CANCELLED -> Icons.Filled.Block to Theme.statusColor("cancelled")
        NotificationKind.LEAVE_CANCELLATION_REQUESTED -> Icons.Filled.FlightTakeoff to orange
        NotificationKind.WFH_REQUESTED -> Icons.Filled.Home to orange
        NotificationKind.WFH_APPROVED -> Icons.Filled.Home to green
        NotificationKind.WFH_REJECTED -> Icons.Filled.Home to red
        NotificationKind.USER_APPROVAL -> Icons.Filled.VerifiedUser to Theme.brand
        NotificationKind.ONBOARDING_SUBMITTED, NotificationKind.ONBOARDING_APPROVED,
        NotificationKind.ONBOARDING_REJECTED, NotificationKind.ONBOARDING_UPDATE -> Icons.Filled.Description to Theme.brand
        NotificationKind.OTHER -> Icons.Filled.NotificationsNone to Theme.brand
    }
}
