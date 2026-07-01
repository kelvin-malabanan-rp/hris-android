package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Dashboard stat tile: tinted icon badge + value + label. Mirrors iOS `StatTile`. */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Theme.Accent,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    ContentCard(modifier = modifier, padding = Theme.Spacing.lg) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(Theme.Size.iconBadge)
                    .background(accent.tint.copy(alpha = Theme.Opacity.fill), RoundedCornerShape(Theme.Radius.control)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent.tint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (detail != null) {
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Placeholder matching [StatTile]'s layout so the dashboard grid doesn't reflow on load. */
@Composable
fun StatTileSkeleton(modifier: Modifier = Modifier) {
    ContentCard(modifier = modifier, padding = Theme.Spacing.lg) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            SkeletonBlock(width = Theme.Size.iconBadge, height = Theme.Size.iconBadge)
            SkeletonBlock(width = 52.dp, height = 22.dp)
            SkeletonBlock(width = 76.dp, height = 12.dp)
        }
    }
}
