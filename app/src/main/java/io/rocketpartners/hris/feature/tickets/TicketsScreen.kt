package io.rocketpartners.hris.feature.tickets

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.Ticket

/** Support tickets list. Mirrors iOS `TicketsView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(
    repository: TicketRepository,
    onOpen: (Ticket) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = remember { TicketsStore(repository) }
    val state by store.state.collectAsState()
    var showNew by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { store.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Support") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = { IconButton(onClick = { showNew = true }) { Icon(Icons.Filled.Add, contentDescription = "New ticket") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val phase = state.phase) {
                is Phase.Failed -> ErrorState(message = phase.message, retry = { store.load() })
                is Phase.Loaded -> if (state.tickets.isEmpty()) {
                    EmptyState(icon = Icons.Filled.ConfirmationNumber, title = "No tickets yet", message = "Raise a support ticket and it'll appear here.", modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl))
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        state.tickets.forEach { TicketCard(it) { onOpen(it) } }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }

    if (showNew) {
        NewTicketSheet(store = store, onDismiss = { showNew = false })
    }
}



@Composable
private fun TicketCard(ticket: Ticket, onClick: () -> Unit) {
    ContentCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ticket.subject ?: "Ticket", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val meta = listOfNotNull(ticket.categoryLabel, ticket.priorityLabel, ticket.messageCount?.let { "$it messages" }).joinToString(" · ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(text = ticket.statusLabel, rawStatus = ticket.status)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
