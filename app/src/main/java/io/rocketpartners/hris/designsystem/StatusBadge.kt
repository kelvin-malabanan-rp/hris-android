package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.rocketpartners.hris.model.humanStatusLabel

/**
 * A compact, color-coded pill for a leave/WFH/ticket status. Pass the human [text]; if the raw enum
 * name differs, pass it as [rawStatus] so the color mapping stays accurate. Mirrors iOS
 * `StatusBadge`.
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    rawStatus: String? = null,
) {
    val color = Theme.statusColor(rawStatus ?: text)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = Theme.Opacity.fill))
            .padding(horizontal = Theme.Spacing.sm, vertical = Theme.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(Theme.Size.statusDot)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = Theme.Spacing.xs),
        )
    }
}

/** Builds a badge from a raw backend status alone, humanizing the label and keeping the raw color. */
@Composable
fun StatusBadge(rawStatus: String, modifier: Modifier = Modifier) {
    StatusBadge(text = humanStatusLabel(rawStatus), modifier = modifier, rawStatus = rawStatus)
}

/** The pill's semantic color for [rawStatus]; exposed for callers that tint their own chrome. */
@Composable
fun statusColorFor(rawStatus: String): Color = Theme.statusColor(rawStatus)
