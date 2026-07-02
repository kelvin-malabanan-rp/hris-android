package io.rocketpartners.hris.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.common.MAX_ATTACHMENTS
import io.rocketpartners.hris.feature.common.rememberImageAttachmentPicker
import io.rocketpartners.hris.model.NewTicket
import io.rocketpartners.hris.model.TicketCategory
import io.rocketpartners.hris.model.TicketPriority
import io.rocketpartners.hris.model.UploadFile
import kotlinx.coroutines.launch

/**
 * Create-ticket sheet: a Cancel/Submit bar, a Subject field, a grouped Details card (Category +
 * Priority dropdowns), a Description field, and an image-attachment picker. Mirrors iOS
 * `NewTicketView`.
 */
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
    var attachments by remember { mutableStateOf<List<UploadFile>>(emptyList()) }
    val pickImages = rememberImageAttachmentPicker { added, _ ->
        attachments = (attachments + added).take(MAX_ATTACHMENTS)
    }
    val canSubmit = subject.isNotBlank() && description.isNotBlank() && !saving

    fun submit() {
        scope.launch {
            saving = true
            store.clearCreateError()
            val ok = store.create(NewTicket(subject.trim(), description.trim(), category, priority), attachments)
            saving = false
            if (ok) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.Spacing.lg).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            // Cancel / title / Submit bar.
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text(
                    "New Ticket",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { submit() }, enabled = canSubmit) { Text("Submit", fontWeight = FontWeight.SemiBold) }
                }
            }

            SectionLabel("Subject")
            OutlinedTextField(subject, { subject = it }, placeholder = { Text("Brief summary") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            SectionLabel("Details")
            GroupCard {
                PickerRow("Category", category.label, TicketCategory.entries, { it.label }) { category = it }
                HorizontalDivider(Modifier.padding(horizontal = Theme.Spacing.lg))
                PickerRow("Priority", priority.label, TicketPriority.entries, { it.label }) { priority = it }
            }

            SectionLabel("Description")
            OutlinedTextField(description, { description = it }, placeholder = { Text("Describe the issue") }, minLines = 4, modifier = Modifier.fillMaxWidth())

            SectionLabel("Attachments")
            GroupCard {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = attachments.size < MAX_ATTACHMENTS, onClick = pickImages)
                        .padding(Theme.Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Theme.brand)
                    Text("Add Photos", style = MaterialTheme.typography.bodyMedium, color = Theme.brand, fontWeight = FontWeight.Medium)
                }
                attachments.forEach { file ->
                    HorizontalDivider(Modifier.padding(horizontal = Theme.Spacing.lg))
                    Row(Modifier.fillMaxWidth().padding(Theme.Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(20.dp))
                        Text("Photo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(file.formattedSize, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { attachments = attachments - file }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove photo", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text("PDF, JPG, PNG, or DOC up to 10 MB each.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            state.createError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Theme.Spacing.sm))
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(Theme.Radius.control)), content = content)
}

/** A grouped row whose trailing value opens a dropdown of [options]. Mirrors the iOS `Picker` row. */
@Composable
private fun <T> PickerRow(label: String, value: String, options: List<T>, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }.padding(Theme.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Theme.brand, fontWeight = FontWeight.Medium)
            Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(18.dp).padding(start = Theme.Spacing.xs))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(labelOf(option)) }, onClick = { onSelect(option); open = false })
            }
        }
    }
}
