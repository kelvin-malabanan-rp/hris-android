package io.rocketpartners.hris.feature.wfh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/**
 * Schedule one or more WFH days in a single submission. On submit the sheet shows a per-day summary
 * (scheduled / sent-for-approval / skipped) from [WfhScheduleClassifier]. Mirrors iOS.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleWfhSheet(
    usage: WfhWeeklyUsage?,
    onDismiss: () -> Unit,
    onSubmit: suspend (List<LocalDate>, String?) -> WfhBatchResult,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val selected = remember { mutableStateListOf<LocalDate>() }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<WfhScheduleClassifier.Result?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            val result = summary
            if (result != null) {
                Text("Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (result.scheduled.isNotEmpty()) {
                    SummarySection("${result.scheduled.size} scheduled", Icons.Filled.CheckCircle, Theme.statusColor("approved")) {
                        result.scheduled.forEach { DayRow(displayWire(it.date), Icons.Filled.CheckCircle, Theme.statusColor("approved")) }
                    }
                }
                if (result.pending.isNotEmpty()) {
                    SummarySection("${result.pending.size} sent for approval", Icons.Filled.HourglassEmpty, Theme.statusColor("pending"), footer = "Over your weekly quota — sent to your manager for approval.") {
                        result.pending.forEach { DayRow(displayWire(it.date), Icons.Filled.HourglassEmpty, Theme.statusColor("pending")) }
                    }
                }
                if (result.skipped.isNotEmpty()) {
                    SummarySection("${result.skipped.size} skipped", Icons.Filled.RemoveCircleOutline, MaterialTheme.colorScheme.onSurfaceVariant, footer = "Already scheduled or in the past.") {
                        result.skipped.forEach { DayRow(it.format(DAY_FMT), Icons.Filled.RemoveCircleOutline, MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            } else {
                Text("Schedule WFH", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                usage?.let { Text("${it.remaining} of ${it.quota} work-from-home days left this week.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                Text("Dates", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                    selected.sorted().forEach { day ->
                        InputChip(selected = true, onClick = { selected.remove(day) }, label = { Text(day.format(DAY_FMT)) }, trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) })
                    }
                    AssistChip(onClick = { showPicker = true }, label = { Text("Add day") }, leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) })
                }

                OutlinedTextField(reason, { reason = it }, label = { Text("Reason (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Button(
                    onClick = {
                        scope.launch {
                            submitting = true
                            error = null
                            when (val r = onSubmit(selected.sorted(), reason.ifEmpty { null })) {
                                is WfhBatchResult.Success -> summary = WfhScheduleClassifier.classify(selected.sorted(), r.created)
                                is WfhBatchResult.Failure -> error = r.message
                            }
                            submitting = false
                        }
                    },
                    enabled = selected.isNotEmpty() && !submitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Schedule")
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }

    if (showPicker) {
        val today = remember { LocalDate.now() }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (d !in selected) selected.add(d)
                    }
                    showPicker = false
                }) { Text("Add") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun SummarySection(title: String, icon: ImageVector, tint: Color, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        content()
        footer?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DayRow(text: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md), modifier = Modifier.padding(start = Theme.Spacing.sm)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun displayWire(wire: String): String =
    io.rocketpartners.hris.core.networking.WireDate.parse(wire)?.let { DAY_FMT.format(it.atZone(ZoneOffset.UTC)) } ?: wire
