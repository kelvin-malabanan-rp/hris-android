package io.rocketpartners.hris.feature.wfh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.common.DateField
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val FIELD_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

/** Bottom sheet to schedule a work-from-home day. Mirrors iOS `ScheduleWfhSheet`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleWfhSheet(
    usage: WfhWeeklyUsage?,
    onDismiss: () -> Unit,
    onSubmit: suspend (LocalDate, String?) -> Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
        ) {
            Text("Schedule WFH", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            usage?.let {
                Text("${it.remaining} of ${it.quota} work-from-home days left this week.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DateField("Date", date, onSelect = { date = it }, formatter = FIELD_FMT)
            OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(
                onClick = {
                    scope.launch {
                        submitting = true
                        val ok = onSubmit(date, reason.ifEmpty { null })
                        submitting = false
                        if (ok) onDismiss()
                    }
                },
                enabled = !submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Schedule")
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}
