package io.rocketpartners.hris.feature.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.HtmlText
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.Announcement

/** A single announcement: title, author/category meta, and the HTML body. Mirrors iOS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    id: Int,
    seed: Announcement?,
    repository: AnnouncementRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = remember { AnnouncementDetailStore(id, seed, repository) }
    val state by store.state.collectAsState()

    LaunchedEffect(Unit) { store.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Announcement") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val announcement = state.announcement
            when {
                announcement != null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                    Text(announcement.title ?: "Untitled", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    val meta = listOfNotNull(announcement.categoryLabel, announcement.authorName, announcement.authorPosition).joinToString(" · ")
                    if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    announcement.body?.let { HtmlText(it, Modifier.fillMaxWidth()) }
                }
                state.phase is Phase.Failed -> ErrorState(message = (state.phase as Phase.Failed).message, retry = { store.load() })
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
