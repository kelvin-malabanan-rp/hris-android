package io.rocketpartners.hris.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.AppNotification
import io.rocketpartners.hris.model.NotificationKind
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HERO_FMT = DateTimeFormatter.ofPattern("EEEE, MMM d 'at' h:mm a", Locale.getDefault())

/**
 * Full-screen detail for one notification: a centered hero (tinted kind icon + title + absolute
 * timestamp) above the full message in a card, plus a contextual action when the notification points
 * at an in-app destination. Mirrors iOS `NotificationDetailView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notification: AppNotification,
    onBack: () -> Unit,
    onRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Notification") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(padding).padding(Theme.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero.
            Column(
                Modifier.fillMaxWidth().padding(top = Theme.Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
            ) {
                KindIcon(notification.kind, size = Theme.Size.fab, glyph = 28.dp)
                Text(notification.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                notification.createdAtDate?.let {
                    Text(
                        it.atZone(ZoneId.systemDefault()).format(HERO_FMT),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ContentCard(Modifier.fillMaxWidth()) {
                Text(notification.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
            }

            actionLabel(notification)?.let { label ->
                Button(onClick = onRoute, modifier = Modifier.fillMaxWidth()) { Text(label, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/**
 * Label for the destination action. Request kinds read "Review request"; everything else mirrors
 * [AppNotification.routedTab]; `null` (no destination) hides the button. Mirrors iOS `actionLabel`.
 */
private fun actionLabel(notification: AppNotification): String? = when (notification.kind) {
    NotificationKind.LEAVE_REQUESTED, NotificationKind.LEAVE_CANCELLATION_REQUESTED, NotificationKind.WFH_REQUESTED -> "Review request"
    else -> when (notification.routedTab) {
        "leave" -> "View in Leave"
        "wfh" -> "View in WFH"
        "calendar" -> "View in Calendar"
        else -> null
    }
}
