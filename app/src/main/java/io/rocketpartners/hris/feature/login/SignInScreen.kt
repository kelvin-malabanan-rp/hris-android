package io.rocketpartners.hris.feature.login

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rocketpartners.hris.R
import io.rocketpartners.hris.designsystem.Haptics
import io.rocketpartners.hris.designsystem.Poppins
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.auth.AuthService
import io.rocketpartners.hris.feature.auth.AuthState
import kotlinx.coroutines.launch

/**
 * SSO-first sign-in screen: Google/Okta buttons above an "or with email" divider, then
 * underline-style email/password fields and the ink-filled "Sign in" capsule. Mirrors iOS
 * `SignInView`.
 */
@Composable
fun SignInScreen(authService: AuthService, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = remember { LoginStore() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    val state by authService.state.collectAsState()
    val errorMessage by authService.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    store.email = email
    store.password = password
    val isAuthenticating = state is AuthState.Authenticating

    // Flag a bad address on blur rather than while the user is mid-type.
    val emailInvalid = store.hasEmailInput && !store.isEmailValid && !emailFocused

    val formAlpha by animateFloatAsState(
        targetValue = if (isAuthenticating) 0.45f else 1f,
        animationSpec = tween(Theme.Motion.quick),
        label = "signin-form-alpha",
    )

    Column(modifier = modifier.fillMaxSize()) {
        BackChevron(
            onBack = onBack,
            modifier = Modifier.padding(horizontal = LoginMetrics.hInset, vertical = Theme.Spacing.sm),
        )

        // Fill the viewport so the form centers vertically; once the keyboard shrinks it,
        // the same content scrolls instead of compressing.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = LoginMetrics.hInset, vertical = Theme.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LoginMetrics.blockGap, Alignment.CenterVertically),
        ) {
            TitleBlock()

            Column(
                verticalArrangement = Arrangement.spacedBy(LoginMetrics.blockGap),
                modifier = Modifier.alpha(formAlpha),
            ) {
                SsoButtons(store = store, enabled = !isAuthenticating)
                OrWithEmailDivider()
                Fields(
                    email = email,
                    onEmailChange = { email = it; authService.clearError() },
                    password = password,
                    onPasswordChange = { password = it; authService.clearError() },
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    emailInvalid = emailInvalid,
                    emailFocused = emailFocused,
                    onEmailFocus = { emailFocused = it },
                    passwordFocused = passwordFocused,
                    onPasswordFocus = { passwordFocused = it },
                    errorMessage = errorMessage,
                    enabled = !isAuthenticating,
                )
            }

            CtaBlock(
                title = if (isAuthenticating) "Signing in…" else "Sign in",
                isBusy = isAuthenticating,
                enabled = store.canSubmit && !isAuthenticating,
                onSubmit = {
                    focusManager.clearFocus()
                    Haptics.impact(view)
                    scope.launch { authService.login(store.trimmedEmail, password) }
                },
            )
        }
    }
}

// --- Header ------------------------------------------------------------------

@Composable
private fun BackChevron(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Theme.Size.circleButton)
            .clickable(onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = LoginPalette.ink,
            modifier = Modifier.size(28.dp),
        )
    }
}

// --- Title -------------------------------------------------------------------

@Composable
private fun TitleBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Text(
            "Welcome back.",
            fontFamily = Poppins,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = LoginPalette.ink,
        )
        Text(
            "Sign in to continue.",
            fontFamily = Poppins,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- SSO ---------------------------------------------------------------------

@Composable
private fun SsoButtons(store: LoginStore, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(LoginMetrics.buttonGap)) {
        // Google button — official sign-in branding; do not restyle.
        SsoCapsule(
            onClick = store::signInWithGoogle,
            enabled = enabled,
            fill = LoginPalette.googleFill,
            borderColor = LoginPalette.googleBorder,
            borderWidth = 1.dp,
        ) {
            Image(
                painterResource(R.drawable.google_g),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(LoginMetrics.ssoIconGap))
            Text(
                "Continue with Google",
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LoginPalette.googleLabel,
            )
        }

        SsoCapsule(
            onClick = store::signInWithOkta,
            enabled = enabled,
            fill = Color.Transparent,
            borderColor = LoginPalette.oktaBorder,
            borderWidth = 1.5.dp,
        ) {
            OktaGlyph()
            Spacer(Modifier.width(LoginMetrics.ssoIconGap))
            Text(
                "Continue with Okta",
                fontFamily = Poppins,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LoginPalette.ink,
            )
        }
    }
}

@Composable
private fun SsoCapsule(
    onClick: () -> Unit,
    enabled: Boolean,
    fill: Color,
    borderColor: Color,
    borderWidth: Dp,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.7f else 1f,
        animationSpec = tween(Theme.Motion.quick),
        label = "sso-press",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LoginMetrics.buttonHeight)
            .alpha(pressAlpha)
            .background(fill, CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * Ring glyph standing in for the Okta mark (outer ⌀15, ring width 3.75 — the mock's 24-viewbox
 * circle-in-circle scaled to 18dp). Swap for an official asset if one lands.
 */
@Composable
private fun OktaGlyph() {
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(15.dp).border(3.75.dp, LoginPalette.ink, CircleShape))
    }
}

// --- Divider -----------------------------------------------------------------

@Composable
private fun OrWithEmailDivider() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LoginMetrics.buttonGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(LoginPalette.hairline))
        Text(
            "or with email",
            fontFamily = Poppins,
            fontSize = 12.sp,
            color = LoginPalette.dividerText,
        )
        Box(Modifier.weight(1f).height(1.dp).background(LoginPalette.hairline))
    }
}

// --- Fields ------------------------------------------------------------------

@Composable
private fun Fields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    emailInvalid: Boolean,
    emailFocused: Boolean,
    onEmailFocus: (Boolean) -> Unit,
    passwordFocused: Boolean,
    onPasswordFocus: (Boolean) -> Unit,
    errorMessage: String?,
    enabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LoginMetrics.fieldGap)) {
        // Email
        Column(verticalArrangement = Arrangement.spacedBy(LoginMetrics.labelGap)) {
            FieldLabel("Email", isError = emailInvalid)
            UnderlinedValue(
                underlineColor = when {
                    emailInvalid -> LoginPalette.error
                    emailFocused -> LoginPalette.ink
                    else -> LoginPalette.underlineRest
                },
                underlineWidth = if (emailInvalid || emailFocused) 2.dp else 1.dp,
            ) {
                LoginTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = "Email Address",
                    keyboardType = KeyboardType.Email,
                    enabled = enabled,
                    onFocusChanged = onEmailFocus,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (emailInvalid) {
                FieldCaption("Enter a valid email address.")
            }
        }

        // Password
        Column(verticalArrangement = Arrangement.spacedBy(LoginMetrics.labelGap)) {
            FieldLabel("Password", isError = false)
            UnderlinedValue(
                underlineColor = when {
                    errorMessage != null -> LoginPalette.error
                    passwordFocused -> LoginPalette.ink
                    else -> LoginPalette.underlineRest
                },
                underlineWidth = if (errorMessage != null || passwordFocused) 2.dp else 1.dp,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LoginTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = "Password",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        enabled = enabled,
                        onFocusChanged = onPasswordFocus,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onTogglePasswordVisible, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            if (errorMessage != null) {
                // The caption is easy to miss with TalkBack; the live region announces the
                // failure when it appears (the iOS VoiceOver announcement's counterpart).
                FieldCaption(
                    errorMessage,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val ink = LoginPalette.ink
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = LoginValueTextStyle.copy(color = ink),
        cursorBrush = SolidColor(ink),
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = LoginValueTextStyle, color = ink.copy(alpha = 0.3f))
                }
                innerTextField()
            }
        },
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
    )
}

/**
 * Shared styling for the underline-style field values: 11dp bottom padding and the animated
 * bottom border (1dp rest → 2dp focused/error). Mirrors iOS `UnderlinedValueStyle`.
 */
@Composable
private fun UnderlinedValue(
    underlineColor: Color,
    underlineWidth: Dp,
    content: @Composable () -> Unit,
) {
    val color by animateColorAsState(underlineColor, tween(Theme.Motion.quick), label = "underline-color")
    val width by animateDpAsState(underlineWidth, tween(Theme.Motion.quick), label = "underline-width")
    Column {
        Box(Modifier.padding(bottom = 11.dp)) { content() }
        Box(Modifier.fillMaxWidth().height(width).background(color))
    }
}

@Composable
private fun FieldLabel(text: String, isError: Boolean) {
    Text(
        text.uppercase(),
        fontFamily = Poppins,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = if (isError) LoginPalette.error else LoginPalette.fieldLabel,
    )
}

@Composable
private fun FieldCaption(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = LoginPalette.error,
            modifier = Modifier.size(14.dp),
        )
        Text(
            message,
            fontFamily = Poppins,
            fontSize = 12.sp,
            color = LoginPalette.error,
        )
    }
}

// --- CTA ---------------------------------------------------------------------

@Composable
private fun CtaBlock(title: String, isBusy: Boolean, enabled: Boolean, onSubmit: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LoginMetrics.ctaLinkGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InkCapsuleButton(
            title = title,
            onClick = onSubmit,
            enabled = enabled,
            isBusy = isBusy,
        )
        Text(
            "Forgot password?",
            fontFamily = Poppins,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .alpha(if (isBusy) 0.4f else 1f)
                .clickable(enabled = !isBusy) {
                    // Forgot-password destination is out of scope for the redesign.
                }
                .padding(horizontal = Theme.Spacing.sm, vertical = Theme.Spacing.xs),
        )
    }
}
