package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * A padded card backed by a Liquid Glass surface. Use for floating content groups — the sign-in
 * form, summary tiles, etc. Mirrors iOS `GlassCard`.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Theme.Radius.card,
    padding: Dp = Theme.Spacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier.liquidGlass(shape).padding(padding),
        content = content,
    )
}

/**
 * A padded card with a solid, opaque surface (NOT glass). Use for blocks of content — stat tiles,
 * section/list cards — so glass stays reserved for chrome accents. Mirrors iOS `ContentCard`.
 */
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Theme.Radius.card,
    padding: Dp = Theme.Spacing.cardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    // A Material Surface (not a bare Box) so content color resolves to onSurface — uncolored text
    // stays legible on both light and dark card surfaces.
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/**
 * A titled card with an optional header action, sitting top-trailing (never buried). Mirrors iOS
 * `DSCard`.
 */
@Composable
fun DSCard(
    title: String,
    modifier: Modifier = Modifier,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ContentCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (actionTitle != null && onAction != null) {
                    TextButton(onClick = onAction) { Text(actionTitle) }
                }
            }
            content()
        }
    }
}
