package io.rocketpartners.hris.feature.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.auth.AuthService

private enum class LoginFlow { ONBOARDING, SIGN_IN }

/**
 * Root pre-auth flow: the "Work, organized." intro screen, then the SSO-first sign-in screen.
 * [startAtSignIn] jumps straight to sign-in (the `--ez signin true` launch extra — the Android
 * analog of the iOS `-signin` argument, for screenshots/previews). Mirrors iOS `LoginView`.
 */
@Composable
fun LoginScreen(authService: AuthService, modifier: Modifier = Modifier, startAtSignIn: Boolean = false) {
    var flow by remember {
        mutableStateOf(if (startAtSignIn) LoginFlow.SIGN_IN else LoginFlow.ONBOARDING)
    }

    AnimatedContent(
        targetState = flow,
        transitionSpec = {
            // Sign-in slides in from the trailing edge over the intro (and back out to it),
            // matching the iOS asymmetric move+fade transition.
            if (targetState == LoginFlow.SIGN_IN) {
                (slideInHorizontally(tween(Theme.Motion.slow)) { it / 3 } + fadeIn(tween(Theme.Motion.slow)))
                    .togetherWith(
                        slideOutHorizontally(tween(Theme.Motion.slow)) { -it / 3 } + fadeOut(tween(Theme.Motion.slow)),
                    )
            } else {
                (slideInHorizontally(tween(Theme.Motion.slow)) { -it / 3 } + fadeIn(tween(Theme.Motion.slow)))
                    .togetherWith(
                        slideOutHorizontally(tween(Theme.Motion.slow)) { it / 3 } + fadeOut(tween(Theme.Motion.slow)),
                    )
            }
        },
        label = "login-flow",
        // The app draws edge-to-edge; these screens have no scaffold, so inset the system bars here.
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) { target ->
        when (target) {
            LoginFlow.ONBOARDING -> IntroScreen(onGetStarted = { flow = LoginFlow.SIGN_IN })
            LoginFlow.SIGN_IN -> SignInScreen(
                authService = authService,
                onBack = { flow = LoginFlow.ONBOARDING },
            )
        }
    }
}
