package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver

/**
 * Approximation of iOS 26 "Liquid Glass": a translucent surface with an optional tint and a
 * hairline stroke, clipped to [shape]. Material3 has no native glass, so this is the Android analog
 * of the iOS `.liquidGlass(in:)` helper — used for chrome accents (cards, floating buttons, chips).
 */
fun Modifier.liquidGlass(shape: Shape, tint: Color? = null): Modifier = composed {
    val base = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val fill = tint?.copy(alpha = Theme.Opacity.fill)?.compositeOver(base) ?: base
    val hairline = MaterialTheme.colorScheme.onSurface.copy(alpha = Theme.Opacity.hairline)
    this
        .clip(shape)
        .background(fill, shape)
        .border(Theme.Stroke.subtle, hairline, shape)
}

/** Full-bleed app background color; the subtle grouped surface iOS uses behind glass cards. */
@Composable
fun appBackgroundColor(): Color = MaterialTheme.colorScheme.background
