package io.rocketpartners.hris.feature.leave

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import io.rocketpartners.hris.feature.common.InlineDatePicker
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
import io.rocketpartners.hris.designsystem.Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

/** Apply for (or edit) leave — mirrors the iOS `ApplyLeaveView`: Cancel/title/Submit bar, a Type
 *  dropdown, remaining-balance line, grouped Dates card, Impact card, and a Reason field. */
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
    // Which date the inline picker is editing ("start"/"end"), or null for the form.
    var editingField by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { store.loadTypes() }
    LaunchedEffect(state.phase) {
        if (state.phase is ApplyPhase.Submitted) { onSaved(); onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.Spacing.lg).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            val editingKey = editingField
            if (editingKey != null) {
                Text(if (editingKey == "start") "Start Date" else "End Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                InlineDatePicker(
                    initial = if (editingKey == "start") state.startDate else state.endDate,
                    onConfirm = { if (editingKey == "start") store.setStart(it) else store.setEnd(it); editingField = null },
                    onCancel = { editingField = null },
                )
                return@Column
            }

            // Cancel / title / Submit bar.
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text(
                    if (store.editingId != null) "Edit Leave" else "Apply for Leave",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                if (state.phase is ApplyPhase.Submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { scope.launch { store.submit() } }, enabled = state.canSubmit) {
                        Text(if (store.editingId != null) "Save" else "Submit", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Leave Type
            SectionLabel("Leave Type")
            TypeRow(state = state, locked = state.typeLocked, onSelect = store::setType)
            state.selectedBalance?.remainingDays?.let { remaining ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                    Icon(Icons.Filled.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text("${formatDays(remaining)} days remaining", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Dates
            SectionLabel("Dates")
            GroupCard {
                DatePillRow("Start", state.startDate) { editingField = "start" }
                HorizontalDivider(Modifier.padding(horizontal = Theme.Spacing.lg))
                DatePillRow("End", state.endDate) { editingField = "end" }
            }

            // Impact
            state.projectedRemaining?.let { projected ->
                SectionLabel("Impact")
                GroupCard {
                    Row(Modifier.fillMaxWidth().padding(Theme.Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        Text("Balance after this request", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${formatDays(projected)} days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (projected < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            if (state.exceedsBalance) Warning("This request exceeds your available balance.")
            state.medicalCertAdvisory?.let { Warning(it) }

            // Reason
            SectionLabel("Reason")
            OutlinedTextField(value = state.reason, onValueChange = store::setReason, placeholder = { Text("Optional") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            if (state.typesUnavailable) Warning("Leave types couldn't be loaded. Try again later.")
            (state.phase as? ApplyPhase.Failed)?.let { Warning(it.message) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Theme.Spacing.sm))
}

@Composable
private fun GroupCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Theme.Radius.control)), content = content)
}

@Composable
private fun TypeRow(state: ApplyLeaveUiState, locked: Boolean, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val name = if (locked) (state.lockedTypeName ?: "Leave") else (state.selectedType?.name ?: "Select")
    Box {
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Theme.Radius.control))
                .clickable(enabled = !locked) { open = true }
                .padding(Theme.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Type", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(name, style = MaterialTheme.typography.bodyMedium, color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else Theme.brand, fontWeight = FontWeight.Medium)
            if (!locked) Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(18.dp).padding(start = Theme.Spacing.xs))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.leaveTypes.forEach { type ->
                DropdownMenuItem(text = { Text(type.name) }, onClick = { onSelect(type.id); open = false })
            }
        }
    }
}

@Composable
private fun DatePillRow(label: String, value: LocalDate, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(Theme.Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value.format(DATE_FMT),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = Theme.Spacing.md, vertical = Theme.Spacing.sm),
        )
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
