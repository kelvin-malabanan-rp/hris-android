package io.rocketpartners.hris.feature.login

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rocketpartners.hris.designsystem.Poppins
import io.rocketpartners.hris.designsystem.Theme

/**
 * Palette for the login/onboarding redesign. These two screens are deliberately flat and
 * type-led — no glass surfaces, no brand blue — so they carry their own ink-based tokens
 * instead of the app-wide styling. Values come from the approved design (design handoff,
 * option 5a) and intentionally win over [Theme] where they differ. Mirrors iOS `LoginPalette`.
 * Pure `(dark: Boolean)` functions plus `@Composable` conveniences, per the house convention.
 */
object LoginPalette {
    /** Primary text, filled CTA, borders, chevron. */
    fun ink(dark: Boolean): Color = if (dark) Color(0xFFF5F5F7) else Color(0xFF16181D)

    /** Label color on top of an ink-filled capsule. */
    fun onInk(dark: Boolean): Color = if (dark) Color(0xFF16181D) else Color.White

    /** Feature-list row numbers ("01"/"02"/"03"). */
    fun listNumber(dark: Boolean): Color =
        if (dark) Color(0xFFEBEBF5).copy(alpha = 0.35f) else Color(0xFF3C3C43).copy(alpha = 0.35f)

    /** Row separators and the "or with email" divider rule. */
    fun hairline(dark: Boolean): Color =
        if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)

    /** Unfocused field underline. */
    fun underlineRest(dark: Boolean): Color =
        if (dark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)

    /** Uppercase field labels. */
    fun fieldLabel(dark: Boolean): Color =
        if (dark) Color(0xFFEBEBF5).copy(alpha = 0.50f) else Color(0xFF3C3C43).copy(alpha = 0.55f)

    /** "or with email" divider text. */
    fun dividerText(dark: Boolean): Color =
        if (dark) Color(0xFFEBEBF5).copy(alpha = 0.50f) else Color(0xFF3C3C43).copy(alpha = 0.50f)

    /** Invalid-email / auth-error underline and captions. */
    fun error(dark: Boolean): Color = if (dark) Color(0xFFFF453A) else Color(0xFFD70015)

    // Google button — official sign-in branding; do not restyle.
    fun googleFill(dark: Boolean): Color = if (dark) Color(0xFF131314) else Color.White
    fun googleBorder(dark: Boolean): Color = if (dark) Color(0xFF8E918F) else Color(0xFF747775)
    fun googleLabel(dark: Boolean): Color = if (dark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)

    fun oktaBorder(dark: Boolean): Color =
        if (dark) Color.White.copy(alpha = 0.5f) else Color(0xFF16181D)

    val ink: Color @Composable @ReadOnlyComposable get() = ink(isSystemInDarkTheme())
    val onInk: Color @Composable @ReadOnlyComposable get() = onInk(isSystemInDarkTheme())
    val listNumber: Color @Composable @ReadOnlyComposable get() = listNumber(isSystemInDarkTheme())
    val hairline: Color @Composable @ReadOnlyComposable get() = hairline(isSystemInDarkTheme())
    val underlineRest: Color @Composable @ReadOnlyComposable get() = underlineRest(isSystemInDarkTheme())
    val fieldLabel: Color @Composable @ReadOnlyComposable get() = fieldLabel(isSystemInDarkTheme())
    val dividerText: Color @Composable @ReadOnlyComposable get() = dividerText(isSystemInDarkTheme())
    val error: Color @Composable @ReadOnlyComposable get() = error(isSystemInDarkTheme())
    val googleFill: Color @Composable @ReadOnlyComposable get() = googleFill(isSystemInDarkTheme())
    val googleBorder: Color @Composable @ReadOnlyComposable get() = googleBorder(isSystemInDarkTheme())
    val googleLabel: Color @Composable @ReadOnlyComposable get() = googleLabel(isSystemInDarkTheme())
    val oktaBorder: Color @Composable @ReadOnlyComposable get() = oktaBorder(isSystemInDarkTheme())
}

/** Fixed dimensions from the 402×874 mock (px map 1:1 to dp). Mirrors iOS `LoginMetrics`. */
object LoginMetrics {
    /** Horizontal content inset for both screens. */
    val hInset = 28.dp

    /** Gap between the major vertically-centered blocks. */
    val blockGap = 32.dp

    /** Gap between the email and password fields. */
    val fieldGap = 24.dp

    /** Gap between the two SSO buttons. */
    val buttonGap = 12.dp

    /** Gap between the CTA and the "Forgot password?" link. */
    val ctaLinkGap = 14.dp

    /** Field label → value gap. */
    val labelGap = 8.dp

    /** Feature-list row vertical padding. */
    val rowVPad = 15.dp

    /** CTA / SSO button height (capsule radius = height / 2). */
    val buttonHeight = 52.dp

    /** SSO / CTA logo-to-label gap. */
    val ssoIconGap = 9.dp
}

/** Shared text style for the 17sp field values / button labels on these screens. */
internal val LoginValueTextStyle = TextStyle(fontFamily = Poppins, fontSize = 17.sp)

/**
 * Ink-filled 52dp capsule CTA ("Get started" / "Sign in"). While busy it swaps the trailing
 * arrow for a spinner; when disabled (and not busy) it dims to 35% per the design. Presses dim
 * the capsule to 70%, the standard feedback for the flat custom buttons on these screens.
 * Mirrors iOS `InkCapsuleButton` + `LoginPressableButtonStyle`.
 */
@Composable
fun InkCapsuleButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBusy: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.7f else 1f,
        animationSpec = tween(Theme.Motion.quick),
        label = "ink-capsule-press",
    )
    val ink = LoginPalette.ink
    val onInk = LoginPalette.onInk

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(LoginMetrics.buttonHeight)
            .alpha(pressAlpha * if (enabled || isBusy) 1f else 0.35f)
            .background(ink, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isBusy,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = LoginValueTextStyle,
            fontWeight = FontWeight.SemiBold,
            color = onInk,
        )
        Spacer(Modifier.width(Theme.Spacing.sm))
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = onInk,
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = onInk,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
