package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Leave-balance tile: a tinted glyph badge, the **remaining** days prominently, an "of {total}"
 * entitlement, and a slim bar filled to `remaining/total` in the leave-type color. Unpaid leave
 * (`total == 0`) renders a full neutral track and no fill. Mirrors iOS `LeaveBalanceTile`.
 */
@Composable
fun LeaveBalanceTile(
    typeName: String,
    remaining: Double?,
    total: Double?,
    accent: Theme.Accent,
    modifier: Modifier = Modifier,
    barColor: Color? = null,
) {
    val tint = barColor ?: accent.tint
    val hasEntitlement = (total ?: 0.0) > 0.0
    val fraction = ProgressMath.fraction(remaining ?: 0.0, total ?: 0.0).toFloat()

    ContentCard(modifier = modifier, padding = Theme.Spacing.lg) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Box(
                modifier = Modifier
                    .size(Theme.Size.iconBadge)
                    .background(tint.copy(alpha = Theme.Opacity.fill), RoundedCornerShape(Theme.Radius.control)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = tint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
                    Text(remaining?.let(::formatBalance) ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (hasEntitlement && total != null) {
                        Text("of ${formatBalance(total)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("days left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Slim capsule bar: neutral track + tinted fill sized to the remaining fraction.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(Theme.Size.progressBar)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = Theme.Opacity.fill)),
            ) {
                if (hasEntitlement) {
                    Box(Modifier.fillMaxWidth(fraction).height(Theme.Size.progressBar).clip(CircleShape).background(tint))
                }
            }
            Text(typeName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** "8" for a whole number, "8.5" otherwise — matches iOS `Double.formatted()`. */
private fun formatBalance(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
