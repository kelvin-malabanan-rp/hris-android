package io.rocketpartners.hris.feature.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.common.DateField
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val FIELD_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

/** Bottom sheet to apply for (or edit) leave. Mirrors iOS `ApplyLeaveView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveSheet(
    repository: LeaveRepository,
    onDismiss: () -> Unit,
    editing: io.rocketpartners.hris.model.LeaveApplication? = null,
    onSaved: () -> Unit,
) {
    val store = remember { ApplyLeaveStore(repository, editing) }
    val state by store.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { store.loadTypes() }
    LaunchedEffect(state.phase) {
        if (state.phase is ApplyPhase.Submitted) {
            onSaved()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
        ) {
            Text(if (store.editingId != null) "Edit Leave" else "Apply for Leave", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (state.typesUnavailable) {
                Warning("Leave types couldn't be loaded. Try again later.")
            }

            // Leave type
            if (state.typeLocked) {
                OutlinedTextField(value = state.lockedTypeName ?: "Leave", onValueChange = {}, readOnly = true, enabled = false, label = { Text("Leave Type") }, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Leave Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    state.leaveTypes.forEach { type ->
                        FilterChip(selected = state.selectedTypeId == type.id, onClick = { store.setType(type.id) }, label = { Text(type.name) })
                    }
                }
            }

            DateField("Start Date", state.startDate, onSelect = store::setStart, formatter = FIELD_FMT)
            DateField("End Date", state.endDate, onSelect = store::setEnd, formatter = FIELD_FMT)

            OutlinedTextField(
                value = state.reason,
                onValueChange = store::setReason,
                label = { Text("Reason (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            // Preview
            Text("${state.requestedDays} day${if (state.requestedDays == 1) "" else "s"} requested", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.projectedRemaining?.let {
                Text("Projected balance after: ${formatDays(it)} days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.exceedsBalance) Warning("This request exceeds your available balance.")
            state.medicalCertAdvisory?.let { Warning(it) }
            (state.phase as? ApplyPhase.Failed)?.let { Warning(it.message) }

            Button(
                onClick = { scope.launch { store.submit() } },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.phase is ApplyPhase.Submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (store.editingId != null) "Save Changes" else "Submit")
                }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun Warning(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

private fun formatDays(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
