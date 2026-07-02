package io.rocketpartners.hris.feature.tickets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.TicketMessage
import kotlinx.coroutines.launch

/** A ticket thread with a reply composer. Support on one side, the employee on the other. Mirrors iOS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(id: Int, repository: TicketRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = remember { TicketDetailStore(repository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var reply by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<io.rocketpartners.hris.model.UploadFile>>(emptyList()) }
    val pickImages = io.rocketpartners.hris.feature.common.rememberImageAttachmentPicker { added, _ ->
        attachments = (attachments + added).take(io.rocketpartners.hris.feature.common.MAX_ATTACHMENTS)
    }

    LaunchedEffect(Unit) { store.load(id) }

    Scaffold(
        modifier = modifier,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(state.detail?.ticket?.subject ?: "Ticket") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val detail = state.detail
            when {
                detail != null -> Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(detail.ticket.subject ?: "Ticket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            StatusBadge(text = detail.ticket.statusLabel, rawStatus = detail.ticket.status)
                        }
                        detail.ticket.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        detail.messages.forEach { MessageBubble(it) }
                    }
                    if (!detail.ticket.isResolved) {
                        Column(Modifier.fillMaxWidth().padding(Theme.Spacing.md), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                            io.rocketpartners.hris.feature.common.AttachmentChips(attachments, onRemove = { f -> attachments = attachments - f })
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
                            ) {
                                IconButton(enabled = attachments.size < io.rocketpartners.hris.feature.common.MAX_ATTACHMENTS, onClick = pickImages) {
                                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedTextField(reply, { reply = it }, placeholder = { Text("Message") }, modifier = Modifier.weight(1f), maxLines = 4)
                                IconButton(
                                    enabled = (reply.isNotBlank() || attachments.isNotEmpty()) && !state.isSending,
                                    onClick = { scope.launch { if (store.reply(reply, attachments)) { reply = ""; attachments = emptyList() } } },
                                ) {
                                    if (state.isSending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Theme.brand)
                                }
                            }
                        }
                    }
                }
                state.phase is Phase.Failed -> ErrorState(message = (state.phase as Phase.Failed).message, retry = { store.load(id) })
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: TicketMessage) {
    val fromSupport = message.isSupport
    val bubbleColor = if (fromSupport) MaterialTheme.colorScheme.surfaceContainerHigh else Theme.brand
    val textColor = if (fromSupport) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.White
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromSupport) Arrangement.Start else Arrangement.End) {
        Column(
            Modifier.widthIn(max = 280.dp).background(bubbleColor, RoundedCornerShape(16.dp)).padding(Theme.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            message.userName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f)) }
            message.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = textColor) }
        }
    }
}
