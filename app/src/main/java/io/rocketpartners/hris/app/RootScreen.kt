package io.rocketpartners.hris.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.auth.AuthState
import io.rocketpartners.hris.feature.login.LoginScreen

/**
 * Auth gate: shows Login while unauthenticated/authenticating and the tab scaffold once
 * authenticated. Restores a session at launch via [AuthService.bootstrap]. Mirrors iOS `RootView`.
 */
@Composable
fun RootScreen(environment: AppEnvironment, initialTab: HrisTab = HrisTab.HOME) {
    val authService = environment.authService
    val state by authService.state.collectAsState()

    LaunchedEffect(Unit) { authService.bootstrap() }

    Crossfade(
        targetState = state is AuthState.Authenticated,
        animationSpec = tween(Theme.Motion.standard),
        label = "auth-gate",
    ) { authenticated ->
        val current = state
        if (authenticated && current is AuthState.Authenticated) {
            MainTabScaffold(environment = environment, currentUser = current.user, initialTab = initialTab)
        } else {
            LoginScreen(authService = authService)
        }
    }
}
