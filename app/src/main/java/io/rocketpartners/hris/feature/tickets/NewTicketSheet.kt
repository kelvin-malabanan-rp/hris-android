package io.rocketpartners.hris.feature.tickets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.NewTicket
import io.rocketpartners.hris.model.TicketCategory
import io.rocketpartners.hris.model.TicketPriority
import kotlinx.coroutines.launch

/** Create-ticket sheet (text only; attachment picker deferred to P6). Mirrors iOS `NewTicketView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTicketSheet(store: TicketsStore, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val state by store.state.collectAsState()

    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TicketCategory.DEFAULT) }
    var priority by remember { mutableStateOf(TicketPriority.DEFAULT) }
    var saving by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            Text("New Ticket", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(subject, { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                TicketCategory.entries.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) }) }
            }
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                TicketPriority.entries.forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) }) }
            }

            state.createError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(
                enabled = subject.isNotBlank() && description.isNotBlank() && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        store.clearCreateError()
                        val ok = store.create(NewTicket(subject.trim(), description.trim(), category, priority), emptyList())
                        saving = false
                        if (ok) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Submit Ticket")
            }
        }
    }
}
