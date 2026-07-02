package io.rocketpartners.hris.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * A circular progress ring with a centered label; the fill sweeps from 0 to its clamped fraction.
 * Mirrors iOS `ProgressRing`.
 */
@Composable
fun ProgressRing(
    value: Double,
    total: Double,
    modifier: Modifier = Modifier,
    tint: Color = Theme.brandLight,
    lineWidth: Dp = Theme.Size.ringLineWidth,
    label: @Composable () -> Unit,
) {
    val fraction = ProgressMath.fraction(value, total).toFloat()
    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(Theme.Motion.slow), label = "ring")
    val strokePx = with(LocalDensity.current) { lineWidth.toPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = tint.copy(alpha = Theme.Opacity.fill),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokePx),
            )
            drawArc(
                color = tint,
                startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Round),
            )
        }
        label()
    }
}
