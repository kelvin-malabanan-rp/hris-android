package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Selected-state treatment: [Tinted] brand-washed surface + stroke; [Solid] fills with the brand. */
enum class FilterChipStyle { Tinted, Solid }

/**
 * Selectable capsule filter chip shared by horizontal filter bars (Calendar event types, the
 * notification inbox). Mirrors iOS `FilterChip`: an optional leading color dot, brand-tinted or
 * brand-solid selected treatment.
 */
@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    style: FilterChipStyle = FilterChipStyle.Tinted,
) {
    val background = when {
        !isSelected -> MaterialTheme.colorScheme.surfaceContainer
        style == FilterChipStyle.Solid -> Theme.brand
        else -> Theme.brand.copy(alpha = Theme.Opacity.surface)
    }
    val foreground = when {
        !isSelected -> MaterialTheme.colorScheme.onSurface
        style == FilterChipStyle.Solid -> Color.White
        else -> Theme.brand
    }
    val strokeColor = if (isSelected && style == FilterChipStyle.Tinted) Theme.brand else Color.Transparent
    Row(
        modifier = modifier
            .background(background, CircleShape)
            .border(Theme.Stroke.subtle, strokeColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.Spacing.md, vertical = Theme.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = foreground)
    }
}
