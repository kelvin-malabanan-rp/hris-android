package io.rocketpartners.hris.feature.timeoff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.LeaveDateText
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.leave.LeaveApprovalsStore
import io.rocketpartners.hris.feature.leave.LeaveRepository
import io.rocketpartners.hris.feature.wfh.WfhApprovalsStore
import io.rocketpartners.hris.feature.wfh.WfhRepository
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.WfhSchedule

/** Combined approver inbox: Leave + WFH pending requests with approve/reject. Mirrors iOS `ApprovalsView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalsScreen(
    leaveRepository: LeaveRepository,
    wfhRepository: WfhRepository,
    canApproveLeave: Boolean,
    canApproveWfh: Boolean,
    preselect: ApprovalKind,
    onBack: () -> Unit,
) {
    val leaveStore = remember { LeaveApprovalsStore(leaveRepository) }
    val wfhStore = remember { WfhApprovalsStore(wfhRepository) }
    val leaveState by leaveStore.state.collectAsState()
    val wfhState by wfhStore.state.collectAsState()

    val showsPicker = canApproveLeave && canApproveWfh
    var kind by remember {
        mutableStateOf(
            when {
                preselect == ApprovalKind.WFH && canApproveWfh -> ApprovalKind.WFH
                preselect == ApprovalKind.LEAVE && canApproveLeave -> ApprovalKind.LEAVE
                canApproveLeave -> ApprovalKind.LEAVE
                else -> ApprovalKind.WFH
            },
        )
    }

    LaunchedEffect(Unit) { if (canApproveLeave) leaveStore.load() }
    LaunchedEffect(Unit) { if (canApproveWfh) wfhStore.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Approvals") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            if (showsPicker) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = kind == ApprovalKind.LEAVE, onClick = { kind = ApprovalKind.LEAVE }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Leave") }
                    SegmentedButton(selected = kind == ApprovalKind.WFH, onClick = { kind = ApprovalKind.WFH }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("WFH") }
                }
            }

            val undoMessage = if (kind == ApprovalKind.LEAVE) leaveState.undoMessage else wfhState.undoMessage
            if (undoMessage != null) {
                UndoBar(undoMessage) { if (kind == ApprovalKind.LEAVE) leaveStore.undo() else wfhStore.undo() }
            }

            if (kind == ApprovalKind.LEAVE) {
                ApprovalList(
                    phase = leaveState.phase,
                    items = leaveState.pending,
                    empty = "No leave requests awaiting approval",
                ) { LeaveApprovalRow(it, onApprove = { leaveStore.approve(it) }, onReject = { leaveStore.reject(it) }) }
            } else {
                ApprovalList(
                    phase = wfhState.phase,
                    items = wfhState.pending,
                    empty = "No WFH requests awaiting approval",
                ) { WfhApprovalRow(it, onApprove = { wfhStore.approve(it) }, onReject = { wfhStore.reject(it) }) }
            }
        }
    }
}

@Composable
private fun <T> ApprovalList(phase: Phase, items: List<T>, empty: String, row: @Composable (T) -> Unit) {
    when {
        items.isNotEmpty() -> items.forEach { row(it) }
        phase is Phase.Loaded -> EmptyState(icon = Icons.Filled.Inbox, title = empty)
        phase is Phase.Failed -> EmptyState(icon = Icons.Filled.Inbox, title = phase.message)
        else -> Box(Modifier.fillMaxWidth().padding(Theme.Spacing.xl), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}

@Composable
private fun UndoBar(message: String, onUndo: () -> Unit) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onUndo) { Text("Undo") }
        }
    }
}

@Composable
private fun LeaveApprovalRow(leave: LeaveApplication, onApprove: () -> Unit, onReject: () -> Unit) {
    ContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(leave.userName ?: "Employee", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${leave.leaveTypeName ?: "Leave"} · ${LeaveDateText.range(leave.startDate, leave.endDate)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(text = leave.displayStatus, rawStatus = leave.status)
            }
            ApproveRejectButtons(onApprove, onReject)
        }
    }
}

@Composable
private fun WfhApprovalRow(schedule: WfhSchedule, onApprove: () -> Unit, onReject: () -> Unit) {
    ContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            Text(schedule.userName ?: "Employee", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${schedule.date}${schedule.reason?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ApproveRejectButtons(onApprove, onReject)
        }
    }
}

@Composable
private fun ApproveRejectButtons(onApprove: () -> Unit, onReject: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Approve") }
        OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) { Text("Reject") }
    }
}
