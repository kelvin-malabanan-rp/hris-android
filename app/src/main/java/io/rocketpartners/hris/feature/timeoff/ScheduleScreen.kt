package io.rocketpartners.hris.feature.timeoff

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.app.AppEnvironment
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.InlineError
import io.rocketpartners.hris.designsystem.LeaveBalanceTile
import io.rocketpartners.hris.designsystem.LeaveDateText
import io.rocketpartners.hris.designsystem.ProgressRing
import io.rocketpartners.hris.designsystem.SkeletonBlock
import io.rocketpartners.hris.designsystem.SkeletonRow
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.designsystem.hexColor
import io.rocketpartners.hris.feature.leave.ApplyLeaveSheet
import io.rocketpartners.hris.feature.leave.LeaveDetailSheet
import io.rocketpartners.hris.feature.leave.LeaveStore
import io.rocketpartners.hris.feature.wfh.ScheduleWfhSheet
import io.rocketpartners.hris.feature.wfh.WfhStore
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.WfhSchedule
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private const val MAX_PREVIEW = 4
private val DAY_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * The combined **Schedule** screen: WFH usage, leave balances, an approvals entry (for approvers),
 * and the merged "My Requests" list, with a FAB to apply for leave or schedule a WFH day. Mirrors
 * iOS `TimeOffView`.
 */
@Composable
fun ScheduleScreen(
    environment: AppEnvironment,
    canApproveLeave: Boolean,
    canApproveWfh: Boolean,
    modifier: Modifier = Modifier,
    onOpenApprovals: (ApprovalKind) -> Unit = {},
) {
    val leaveStore = remember { LeaveStore(environment.leaveRepository) }
    val wfhStore = remember { WfhStore(environment.wfhRepository) }
    val leave by leaveStore.state.collectAsState()
    val wfh by wfhStore.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        launch { leaveStore.load() }
        launch { wfhStore.load() }
    }

    var showApply by remember { mutableStateOf(false) }
    var showWfh by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var fabMenu by remember { mutableStateOf(false) }
    var cancelWfh by remember { mutableStateOf<WfhSchedule?>(null) }
    var leaveDetail by remember { mutableStateOf<LeaveApplication?>(null) }

    val requests = remember(leave.leaves, wfh.schedules) { ScheduleRequest.merge(leave.leaves, wfh.schedules) }
    val leaveLoading = leave.phase.isPending()
    val wfhLoading = wfh.phase.isPending()

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xl),
        ) {
            WfhSection(wfh, wfhLoading) { scope.launch { wfhStore.load() } }
            BalancesSection(leave.balances, leaveLoading)
            if (canApproveLeave || canApproveWfh) {
                ApprovalsEntry { onOpenApprovals(if (canApproveLeave) ApprovalKind.LEAVE else ApprovalKind.WFH) }
            }
            MyRequestsSection(
                requests = requests,
                showAll = showAll,
                onToggleShowAll = { showAll = !showAll },
                loading = leaveLoading || wfhLoading,
                error = (leave.phase as? Phase.Failed)?.message ?: (wfh.phase as? Phase.Failed)?.message,
                onRetry = { scope.launch { leaveStore.load(); wfhStore.load() } },
                onLeaveClick = { leaveDetail = it },
                onWfhCancel = { cancelWfh = it },
                onApplyLeave = { showApply = true },
                onScheduleWfh = { showWfh = true },
            )
        }

        Box(Modifier.align(Alignment.BottomEnd).padding(Theme.Spacing.xl)) {
            FloatingActionButton(onClick = { fabMenu = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add request")
            }
            DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                DropdownMenuItem(text = { Text("Apply for Leave") }, onClick = { fabMenu = false; showApply = true })
                DropdownMenuItem(text = { Text("Schedule WFH") }, onClick = { fabMenu = false; showWfh = true })
            }
        }
    }

    if (showApply) {
        ApplyLeaveSheet(environment.leaveRepository, onDismiss = { showApply = false }) {
            scope.launch { leaveStore.load() }
        }
    }
    if (showWfh) {
        ScheduleWfhSheet(
            usage = wfh.usage,
            onDismiss = { showWfh = false },
            onSubmit = { date, reason -> wfhStore.scheduleDay(date, reason) },
        )
    }
    leaveDetail?.let { target ->
        LeaveDetailSheet(
            leave = target,
            onDismiss = { leaveDetail = null },
            onCancel = { scope.launch { leaveStore.cancel(target) }; leaveDetail = null },
            onRequestCancellation = { reason -> scope.launch { leaveStore.requestCancellation(target, reason) }; leaveDetail = null },
        )
    }
    cancelWfh?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelWfh = null },
            title = { Text("Cancel WFH day?") },
            text = { Text("This work-from-home day will be cancelled.") },
            confirmButton = { TextButton(onClick = { scope.launch { wfhStore.cancel(target) }; cancelWfh = null }) { Text("Cancel WFH") } },
            dismissButton = { TextButton(onClick = { cancelWfh = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun WfhSection(state: io.rocketpartners.hris.feature.wfh.WfhUiState, loading: Boolean, onRetry: () -> Unit) {
    SectionHeader("Work From Home")
    when {
        state.phase is Phase.Failed -> ContentCard { InlineError((state.phase as Phase.Failed).message) { onRetry() } }
        state.usage == null && state.schedules.isEmpty() && loading -> SkeletonBlock(Modifier.fillMaxWidth(), height = 132.dp)
        else -> WfhUsageCard(state.usage, state.schedules.size)
    }
}

@Composable
private fun WfhUsageCard(usage: WfhWeeklyUsage?, monthCount: Int) {
    ContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            if (usage != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(value = usage.remaining.toDouble(), total = usage.quota.toDouble(), tint = Theme.Accent.WFH.tint, modifier = Modifier.size(96.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${usage.remaining}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("of ${usage.quota}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
                        Text("This Week", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${usage.remaining} remaining", style = MaterialTheme.typography.titleMedium, color = Theme.Accent.WFH.tint)
                        Text("You've used ${usage.used} of ${usage.quota} work-from-home days this week.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (usage.pending > 0) {
                            Text("${usage.pending} awaiting approval", style = MaterialTheme.typography.bodySmall, color = Theme.Accent.PENDING.tint, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                Box(Modifier.size(Theme.Size.iconBadge).background(Theme.Accent.WFH.tint.copy(alpha = Theme.Opacity.fill), RoundedCornerShape(Theme.Radius.control)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Theme.Accent.WFH.tint)
                }
                Column {
                    Text("This Month", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (monthCount == 1) "1 WFH day" else "$monthCount WFH days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun BalancesSection(balances: List<LeaveBalance>, loading: Boolean) {
    if (balances.isNotEmpty()) {
        SectionHeader("Leave Balances")
        val featured = balances.filter { isFeatured(it) }.ifEmpty { balances }
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            featured.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                    row.forEach { b ->
                        LeaveBalanceTile(
                            typeName = b.leaveTypeName ?: "Leave",
                            remaining = b.remainingDays,
                            total = b.totalDays,
                            accent = Theme.Accent.LEAVE,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    } else if (loading) {
        SectionHeader("Balances")
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            SkeletonBlock(Modifier.fillMaxWidth(), height = 60.dp)
            SkeletonBlock(Modifier.fillMaxWidth(), height = 60.dp)
        }
    }
}

private fun isFeatured(b: LeaveBalance): Boolean {
    val n = (b.leaveTypeName ?: "").lowercase()
    return n.contains("annual") || n.contains("vacation") || n.contains("sick") || n.contains("medical")
}

@Composable
private fun ApprovalsEntry(onClick: () -> Unit) {
    ContentCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Icon(Icons.Filled.Inbox, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(Theme.Size.iconInline))
            Text("Pending Approvals", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MyRequestsSection(
    requests: List<ScheduleRequest>,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onLeaveClick: (LeaveApplication) -> Unit,
    onWfhCancel: (WfhSchedule) -> Unit,
    onApplyLeave: () -> Unit,
    onScheduleWfh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("My Requests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (requests.size > MAX_PREVIEW) {
                TextButton(onClick = onToggleShowAll) { Text(if (showAll) "Show less" else "View all") }
            }
        }
        when {
            requests.isNotEmpty() -> {
                val shown = if (showAll) requests else requests.take(MAX_PREVIEW)
                shown.forEach { request ->
                    when (request) {
                        is ScheduleRequest.Leave -> MyRequestLeaveRow(request.leave) { onLeaveClick(request.leave) }
                        is ScheduleRequest.Wfh -> MyRequestWfhRow(request.schedule) { onWfhCancel(request.schedule) }
                    }
                }
            }
            loading -> {
                SkeletonRow(showsLeadingCircle = false); SkeletonRow(showsLeadingCircle = false)
            }
            error != null -> ContentCard { InlineError(error) { onRetry() } }
            else -> RequestsEmptyState(onApplyLeave, onScheduleWfh)
        }
    }
}

@Composable
private fun RequestsEmptyState(onApplyLeave: () -> Unit, onScheduleWfh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        EmptyState(
            icon = Icons.Filled.CalendarMonth,
            title = "No requests yet",
            message = "Apply for leave or schedule a work-from-home day to get started.",
            accent = Theme.Accent.LEAVE,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            androidx.compose.material3.Button(onClick = onApplyLeave, modifier = Modifier.weight(1f)) { Text("Apply for Leave") }
            androidx.compose.material3.OutlinedButton(onClick = onScheduleWfh, modifier = Modifier.weight(1f)) { Text("Schedule WFH") }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun RequestTypeChip(kind: ScheduleRequest.Kind) {
    val tint = if (kind == ScheduleRequest.Kind.LEAVE) Theme.Accent.LEAVE.tint else Theme.Accent.WFH.tint
    Text(
        if (kind == ScheduleRequest.Kind.LEAVE) "LEAVE" else "WFH",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = tint,
        modifier = Modifier.background(tint.copy(alpha = Theme.Opacity.fill), CircleShape).padding(horizontal = Theme.Spacing.sm, vertical = 2.dp),
    )
}

@Composable
private fun MyRequestLeaveRow(leave: LeaveApplication, onClick: () -> Unit) {
    ContentCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Box(Modifier.size(width = 4.dp, height = 44.dp).background(hexColor(leave.leaveTypeColor) ?: Theme.brand, RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
                RequestTypeChip(ScheduleRequest.Kind.LEAVE)
                Text(leave.leaveTypeName ?: "Leave", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(LeaveDateText.range(leave.startDate, leave.endDate), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(text = leave.displayStatus, rawStatus = leave.status)
        }
    }
}

@Composable
private fun MyRequestWfhRow(schedule: WfhSchedule, onCancel: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Box(Modifier.size(width = 4.dp, height = 44.dp).background(Theme.Accent.WFH.tint, RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
                RequestTypeChip(ScheduleRequest.Kind.WFH)
                Text(displayDate(schedule), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                schedule.dayName?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                schedule.managerComments?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            schedule.status?.let { StatusBadge(rawStatus = it) }
            Box {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Cancel WFH") }, onClick = { menu = false; onCancel() })
                }
            }
        }
    }
}

private fun displayDate(schedule: WfhSchedule): String =
    schedule.parsedDate?.let { DAY_FMT.format(it.atZone(ZoneOffset.UTC)) } ?: schedule.date

private fun Phase.isPending(): Boolean = this is Phase.Idle || this is Phase.Loading

/** Which approvals inbox to preselect when the entry is tapped. */
enum class ApprovalKind { LEAVE, WFH }
