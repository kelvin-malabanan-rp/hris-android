package io.rocketpartners.hris.feature.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.rocketpartners.hris.designsystem.LeaveDateText
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.LeaveApplication

/**
 * Detail sheet for a leave application with the employee action derived from its status: cancel a
 * pending request, or request cancellation of an approved one. Mirrors iOS `LeaveDetailView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveDetailSheet(
    leave: LeaveApplication,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRequestCancellation: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reason by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            Text(leave.leaveTypeName ?: "Leave", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            StatusBadge(text = leave.displayStatus, rawStatus = leave.status)
            DetailRow("Dates", LeaveDateText.range(leave.startDate, leave.endDate))
            leave.totalDays?.let { DetailRow("Total days", if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()) }
            leave.reason?.takeIf { it.isNotEmpty() }?.let { DetailRow("Reason", it) }

            when (leave.availableAction) {
                LeaveApplication.Action.CANCEL ->
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Cancel Request") }

                LeaveApplication.Action.REQUEST_CANCELLATION -> {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Cancellation reason") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Button(
                        onClick = { onRequestCancellation(reason) },
                        enabled = reason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Request Cancellation") }
                }

                LeaveApplication.Action.NONE -> Unit
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
