package io.rocketpartners.hris.feature.announcements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.Announcement
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("COMPANY_NEWS", "EVENTS", "FUN", "HR_UPDATES", "GENERAL")

/** Company announcements feed with a category filter. Mirrors iOS `AnnouncementsView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    repository: AnnouncementRepository,
    onOpen: (Announcement) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = remember { AnnouncementsStore(repository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { store.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Announcements") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val phase = state.phase) {
                is Phase.Failed -> ErrorState(message = phase.message, retry = { store.load() })
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                        FilterChip(selected = state.selectedCategory == null, onClick = { scope.launch { store.select(null) } }, label = { Text("All") })
                        CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick = { scope.launch { store.select(cat) } },
                                label = { Text(cat.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }) },
                            )
                        }
                    }
                    if (state.phase is Phase.Loading && state.announcements.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(Theme.Spacing.xl), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else if (state.announcements.isEmpty()) {
                        EmptyState(icon = Icons.Filled.Campaign, title = "No announcements", modifier = Modifier.fillMaxWidth().padding(Theme.Spacing.xl))
                    } else {
                        state.announcements.forEach { AnnouncementCard(it) { onOpen(it) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement, onClick: () -> Unit) {
    ContentCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Icon(
                if (announcement.pinned) Icons.Filled.PushPin else Icons.Filled.Campaign,
                contentDescription = null,
                tint = if (announcement.pinned) Theme.Accent.PENDING.tint else Theme.brand,
                modifier = Modifier.size(Theme.Size.iconInline),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(announcement.title ?: "Untitled", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val meta = listOfNotNull(announcement.categoryLabel, announcement.authorName).joinToString(" · ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
