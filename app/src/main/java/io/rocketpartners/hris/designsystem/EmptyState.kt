package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Branded empty state: a tinted glyph in a soft accent circle, a title, and an optional message.
 * Use [compact] inside cards; full/large elsewhere. Mirrors iOS `EmptyStateView`.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    accent: Theme.Accent = Theme.Accent.INFO,
    compact: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val glyphSize = if (compact) 44.dp else 64.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) Theme.Spacing.sm else Theme.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) Theme.Spacing.sm else Theme.Spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(glyphSize)
                .background(accent.tint.copy(alpha = Theme.Opacity.fill), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent.tint)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs),
        ) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                Text(
                    text = message,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = Theme.Spacing.sm)) {
                Text(actionLabel)
            }
        }
    }
}
