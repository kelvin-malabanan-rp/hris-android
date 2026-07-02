package io.rocketpartners.hris.feature.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.rocketpartners.hris.core.networking.ImageUrls
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.Avatar
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
                    // Author avatar + category/name/position meta (author image is an authed `/uploads` path).
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                        Avatar(name = announcement.authorName, model = announcement.authorImageUrl, size = 36.dp)
                        val meta = listOfNotNull(announcement.categoryLabel, announcement.authorName, announcement.authorPosition).joinToString(" · ")
                        if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    announcement.body?.let { HtmlText(it, Modifier.fillMaxWidth()) }
                    // Attached images — authenticated `/uploads` paths, loaded via the global authed loader.
                    announcement.sortedImagePaths.forEach { path ->
                        AsyncImage(
                            model = ImageUrls.resolve(path),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Theme.Radius.card)),
                        )
                    }
                }
                state.phase is Phase.Failed -> ErrorState(message = (state.phase as Phase.Failed).message, retry = { store.load() })
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}
