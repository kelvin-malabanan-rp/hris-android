package io.rocketpartners.hris.feature.wfh

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.calendar.MonthGrid
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/**
 * Schedule one or more WFH days via an inline multi-select calendar (mirrors the iOS
 * `MultiDatePicker`); on submit the sheet shows a per-day summary (scheduled / sent-for-approval /
 * skipped) from [WfhScheduleClassifier]. Mirrors iOS `ScheduleWfhSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

                Text("Dates", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MultiSelectCalendar(selected = selected) { day ->
                    if (day in selected) selected.remove(day) else selected.add(day)
                }
                if (selected.isNotEmpty()) {
                    Text("${selected.size} day${if (selected.size == 1) "" else "s"} selected", style = MaterialTheme.typography.bodyMedium)
                }
                usage?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                        Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = Theme.Accent.WFH.tint, modifier = Modifier.size(18.dp))
                        Text("${it.remaining} of ${it.quota} WFH days left this week", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("Reason", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(reason, { reason = it }, placeholder = { Text("Optional") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                Button(
                    onClick = {
                        scope.launch {
                            submitting = true
                            error = null
                            val dates = selected.sorted()
                            when (val r = onSubmit(dates, reason.ifEmpty { null })) {
                                is WfhBatchResult.Success -> summary = WfhScheduleClassifier.classify(dates, r.created)
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
}

/** Inline multi-select month calendar; past days are disabled (server skips them). Mirrors iOS MultiDatePicker. */
@Composable
private fun MultiSelectCalendar(selected: List<LocalDate>, onToggle: (LocalDate) -> Unit) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val firstDow = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = Theme.brand) }
            Text(month.atDay(1).format(MONTH_FMT), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = Theme.brand) }
        }
        Row(Modifier.fillMaxWidth()) {
            MonthGrid.weekdaySymbols(firstDow).forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        MonthGrid.weeks(month, firstDow).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val inMonth = day.monthValue == month.monthValue
                    val past = day.isBefore(today)
                    val isSel = day in selected
                    Box(
                        Modifier.weight(1f).padding(vertical = 2.dp).clickable(enabled = inMonth && !past) { onToggle(day) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(36.dp).then(if (isSel) Modifier.background(Theme.brand, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${day.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSel -> Color.White
                                    !inMonth || past -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, icon: ImageVector, tint: Color, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        content()
        footer?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun DayRow(text: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md), modifier = Modifier.padding(start = Theme.Spacing.sm)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun displayWire(wire: String): String =
    io.rocketpartners.hris.core.networking.WireDate.parse(wire)?.let { DAY_FMT.format(it.atZone(ZoneOffset.UTC)) } ?: wire
