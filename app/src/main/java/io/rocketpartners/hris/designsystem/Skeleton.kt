package io.rocketpartners.hris.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A horizontally sweeping highlight that signals "loading". Mirrors iOS `shimmering()`. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmer-translate",
    )
    val highlight = Color.White.copy(alpha = 0.30f)
    val brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, highlight, Color.Transparent),
        start = Offset(translate * 200f, 0f),
        end = Offset(translate * 200f + 400f, 0f),
    )
    this.background(brush)
}

@Composable
private fun skeletonColor(): Color =
    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

/** A single shimmering placeholder bar. Decorative. Mirrors iOS `SkeletonBlock`. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    cornerRadius: Dp = Theme.Radius.control,
) {
    var m = modifier.clip(RoundedCornerShape(cornerRadius)).background(skeletonColor())
    if (width != null) m = m.width(width)
    if (height != null) m = m.height(height)
    Box(modifier = m.shimmer())
}

/** A placeholder list row: leading circle (avatar) + two text bars. Mirrors iOS `SkeletonRow`. */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    showsLeadingCircle: Boolean = true,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
    ) {
        if (showsLeadingCircle) {
            Box(
                modifier = Modifier
                    .size(Theme.Size.iconBadge)
                    .clip(CircleShape)
                    .background(skeletonColor())
                    .shimmer(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
            SkeletonBlock(width = 140.dp, height = 14.dp)
            SkeletonBlock(width = 90.dp, height = 10.dp)
        }
    }
}
