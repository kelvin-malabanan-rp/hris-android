package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * App-wide design tokens: spacing, corner radii, the brand accent, and the mapping from backend
 * status strings to semantic colors, so every screen stays visually consistent. Mirrors iOS
 * `Theme`. Dp/Float tokens are plain constants; color choices that depend on light/dark are exposed
 * as pure `(…, dark: Boolean)` functions (unit-testable) plus `@Composable` conveniences.
 */
object Theme {
    object Spacing {
        val xxs = 2.dp
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val cardPadding = 16.dp
        /** Distance of the floating + button from the screen bottom. */
        val fabBottomInset = 58.dp
        /** Bottom inset for scroll content so the last card clears the floating + button. */
        val fabClearance = 120.dp
        /** Top inset for title-less screens so hero content clears the floating bell. */
        val bellClearance = 88.dp
    }

    object Radius {
        val control = 12.dp
        val card = 20.dp
    }

    /** Stroke widths. [subtle] for dividers/resting outlines, [focus] for selected/active. */
    object Stroke {
        val subtle = 1.dp
        val focus = 1.5.dp
    }

    /**
     * Fixed control dimensions — chrome (icons, rings, dots), not text.
     */
    object Size {
        val fab = 56.dp
        val circleButton = 44.dp
        val iconBadge = 36.dp
        val iconInline = 24.dp
        val ringLineWidth = 10.dp
        val statusDot = 6.dp
        val unreadDot = 8.dp
        val progressBar = 6.dp
    }

    /** Named opacities so tint/fill washes stay consistent. */
    object Opacity {
        const val hairline = 0.08f
        const val fill = 0.15f
        const val surface = 0.18f
        const val inputFill = 0.5f
        const val inputBorder = 0.08f
        const val errorFill = 0.06f
        const val errorBorder = 0.12f
    }

    /** Motion durations (milliseconds). */
    object Motion {
        const val quick = 200
        const val standard = 280
        const val slow = 400
    }

    val brandLight = Color(0xFF007AFF)
    val brandDark = Color(0xFF0A84FF)

    /** The single brand blue, resolved for the current color scheme. */
    val brand: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) brandDark else brandLight

    /** Semantic accent tints mapped from the PWA dashboard tiles. */
    enum class Accent(val tint: Color) {
        INFO(Color(0xFF0A84FF)),
        LEAVE(Color(0xFFAF52DE)),
        PENDING(Color(0xFFFF9500)),
        WFH(Color(0xFF34C759)),
        DANGER(Color(0xFFFF3B30)),
    }

    /**
     * Semantic color for a backend status string (LeaveStatus / WFH status), tolerant of label vs.
     * enum-name and case differences. Light/dark hex pairs are tuned for WCAG-AA text on a 0.15
     * tinted pill. Mirrors iOS `Theme.statusColor`. Pure — [dark] selects the variant.
     */
    fun statusColor(raw: String, dark: Boolean): Color {
        val value = raw.uppercase()
        return when {
            value.contains("APPROV") ->
                if (dark) Color(0xFF34D399) else Color(0xFF065F46)
            value.contains("REJECT") || value.contains("DECLIN") ->
                if (dark) Color(0xFFF87171) else Color(0xFFB91C1C)
            // Deliberate grey that reads "done"/archived — not a disabled secondary.
            value.contains("CANCEL") ->
                if (dark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
            value.contains("PENDING") ->
                if (dark) Color(0xFFFB923C) else Color(0xFF9A3412)
            else -> if (dark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        }
    }

    /** [statusColor] resolved for the current color scheme. */
    @Composable
    @ReadOnlyComposable
    fun statusColor(raw: String): Color = statusColor(raw, isSystemInDarkTheme())
}

// iOS "grouped" palette: a light-grey background with white cards (and the dark-mode inverse),
// so cards read as distinct surfaces the way `systemGroupedBackground`/`secondarySystemGrouped`
// do on iOS. ContentCard/DSCard use `surfaceContainer`, so it is pinned to the card color.
private val LightColors = lightColorScheme(
    primary = Theme.brandLight,
    onPrimary = Color.White,
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1C1E),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerLow = Color.White,
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6B7280),
    error = Color(0xFFD11507),
    outline = Color(0xFFC6C6C8),
)
private val DarkColors = darkColorScheme(
    primary = Theme.brandDark,
    onPrimary = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F7),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    error = Color(0xFFFF453A),
    outline = Color(0xFF48484A),
)

/** Material3 theme wrapper; sets the brand accent as the color-scheme primary. */
@Composable
fun HrisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = scheme,
        typography = HrisTypography,
    ) {
        // Root Surface so LocalContentColor defaults to onBackground app-wide (otherwise uncolored
        // text falls back to black and is invisible on dark surfaces).
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = scheme.background,
            content = content,
        )
    }
}
