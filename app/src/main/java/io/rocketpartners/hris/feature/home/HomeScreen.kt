package io.rocketpartners.hris.feature.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.app.AppEnvironment
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.Avatar
import io.rocketpartners.hris.designsystem.DSCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.Greeting
import io.rocketpartners.hris.designsystem.InlineError
import io.rocketpartners.hris.designsystem.LeaveDateText
import io.rocketpartners.hris.designsystem.SkeletonRow
import io.rocketpartners.hris.designsystem.StatTile
import io.rocketpartners.hris.designsystem.StatTileSkeleton
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.designsystem.hexColor
import io.rocketpartners.hris.model.Announcement
import io.rocketpartners.hris.model.LeaveApplication
import java.time.LocalTime

/**
 * Dashboard: greeting + avatar, four stat tiles, recent leave requests, and announcements. Tiles
 * switch tabs rather than push, matching iOS. Mirrors iOS `HomeView`.
 */
@Composable
fun HomeScreen(
    environment: AppEnvironment,
    modifier: Modifier = Modifier,
    onOpenTimeOff: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
) {
    val store = remember {
        HomeStore(
            leave = environment.leaveRepository,
            wfh = environment.wfhRepository,
            calendar = environment.calendarRepository,
            profile = environment.profileRepository,
            announcements = environment.announcementRepository,
        )
    }
    val state by store.state.collectAsState()
    LaunchedEffect(Unit) { store.load() }

    val phase = state.phase
    if (phase is Phase.Failed) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorState(message = phase.message, retry = { store.load() })
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.Spacing.lg)
            // Extra top inset so the greeting + avatar clear the floating notification bell.
            .padding(top = Theme.Spacing.bellClearance, bottom = Theme.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
    ) {
        Header(state = state, onOpenProfile = onOpenProfile)
        StatTiles(state = state, onOpenTimeOff = onOpenTimeOff, onOpenCalendar = onOpenCalendar)
        RecentRequestsCard(state = state, onOpenTimeOff = onOpenTimeOff, onRetry = { store.load() })
        AnnouncementsCard(state = state, onRetry = { store.load() })
    }
}

@Composable
private fun Header(state: HomeUiState, onOpenProfile: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
            Text(
                Greeting.text(LocalTime.now().hour),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.profile?.firstName ?: state.profile?.displayName ?: "Welcome",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Box(Modifier.clickable(onClick = onOpenProfile)) {
            Avatar(name = state.profile?.displayName, model = state.profile?.profileImageUrl, size = 48.dp)
        }
    }
}

@Composable
private fun StatTiles(state: HomeUiState, onOpenTimeOff: () -> Unit, onOpenCalendar: () -> Unit) {
    val leavePending = state.leavePhase.isPending()
    val wfhPending = state.wfhPhase.isPending()
    val calendarPending = state.calendarPhase.isPending()
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            Tile(Modifier.weight(1f), leavePending, onOpenTimeOff) {
                StatTile("Leave Balance", state.leaveBalanceRemaining?.let { formatDays(it) } ?: "—", Icons.Filled.CalendarMonth, Theme.Accent.LEAVE, detail = "days")
            }
            Tile(Modifier.weight(1f), leavePending, onOpenTimeOff) {
                StatTile("Pending Requests", "${state.pendingCount}", Icons.Filled.Schedule, Theme.Accent.PENDING)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            Tile(Modifier.weight(1f), wfhPending, onOpenTimeOff) {
                StatTile("WFH This Week", state.weeklyUsage?.let { "${it.used}" } ?: "—", Icons.Filled.Home, Theme.Accent.WFH, detail = state.weeklyUsage?.let { "of ${it.quota}" })
            }
            Tile(Modifier.weight(1f), calendarPending, onOpenCalendar) {
                StatTile("Out Today", "${state.outToday.size}", Icons.Filled.Groups, Theme.Accent.INFO)
            }
        }
    }
}

@Composable
private fun Tile(modifier: Modifier, pending: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier.clickable(onClick = onClick)) {
        if (pending) StatTileSkeleton() else content()
    }
}

@Composable
private fun RecentRequestsCard(state: HomeUiState, onOpenTimeOff: () -> Unit, onRetry: suspend () -> Unit) {
    DSCard(title = "Recent Requests", actionTitle = "View all", onAction = onOpenTimeOff) {
        when (val phase = state.leavePhase) {
            is Phase.Idle, is Phase.Loading ->
                Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                    SkeletonRow(showsLeadingCircle = false); SkeletonRow(showsLeadingCircle = false)
                }
            is Phase.Failed -> InlineError(message = phase.message, retry = onRetry)
            is Phase.Loaded ->
                if (state.recentRequests.isEmpty()) {
                    EmptyState(icon = Icons.Filled.Inbox, title = "No recent requests", compact = true)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        state.recentRequests.forEach { RequestRow(it, onOpenTimeOff) }
                    }
                }
        }
    }
}

@Composable
private fun RequestRow(leave: LeaveApplication, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
    ) {
        Box(Modifier.size(width = 4.dp, height = 36.dp).background(hexColor(leave.leaveTypeColor) ?: Theme.brand, RoundedCornerShape(2.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
            Text(leave.leaveTypeName ?: "Leave", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(LeaveDateText.range(leave.startDate, leave.endDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusBadge(text = leave.displayStatus, rawStatus = leave.status)
    }
}

@Composable
private fun AnnouncementsCard(state: HomeUiState, onRetry: suspend () -> Unit) {
    DSCard(title = "Announcements") {
        when (val phase = state.announcementsPhase) {
            is Phase.Idle, is Phase.Loading ->
                Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) { SkeletonRow(); SkeletonRow() }
            is Phase.Failed -> InlineError(message = phase.message, retry = onRetry)
            is Phase.Loaded ->
                if (state.announcements.isEmpty()) {
                    EmptyState(icon = Icons.Filled.Campaign, title = "No announcements", compact = true)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        state.announcements.forEach { AnnouncementRow(it) }
                    }
                }
        }
    }
}

@Composable
private fun AnnouncementRow(announcement: Announcement) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        Icon(
            if (announcement.pinned) Icons.Filled.PushPin else Icons.Filled.Campaign,
            contentDescription = null,
            tint = if (announcement.pinned) Theme.Accent.PENDING.tint else Theme.brand,
            modifier = Modifier.size(Theme.Size.iconInline),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
            Text(announcement.title ?: "Untitled", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            announcement.categoryLabel?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Phase.isPending(): Boolean = this is Phase.Idle || this is Phase.Loading

/** Trims a whole-number balance to "8", keeps "8.5" otherwise — matches iOS `Double.formatted()`. */
private fun formatDays(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
