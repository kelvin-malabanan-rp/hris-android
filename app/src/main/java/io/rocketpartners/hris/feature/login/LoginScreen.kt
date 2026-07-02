package io.rocketpartners.hris.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.GlassCard
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.auth.AuthService
import io.rocketpartners.hris.feature.auth.AuthState
import kotlinx.coroutines.launch

private enum class LoginFlow { ONBOARDING, SIGN_IN }

private data class OnboardingSlide(val title: String, val description: String)

private val slides = listOf(
    OnboardingSlide("Smart Calendar", "Stay in sync with your team. Track schedules, meetings, and availability in one unified calendar."),
    OnboardingSlide("Seamless Leaves", "Apply for leaves with a few taps. Get instant approval updates and track your balance in real time."),
    OnboardingSlide("Flexible WFH", "Coordinate your work-from-home days. Log your location and sync with your team seamlessly."),
)

/** Root login flow: onboarding intro then the sign-in form. Mirrors iOS `LoginView`. */
@Composable
fun LoginScreen(authService: AuthService, modifier: Modifier = Modifier) {
    var flow by remember { mutableStateOf(LoginFlow.SIGN_IN) }
    Box(modifier = modifier.fillMaxSize()) {
        when (flow) {
            LoginFlow.ONBOARDING -> Onboarding(onFinish = { flow = LoginFlow.SIGN_IN })
            LoginFlow.SIGN_IN -> SignIn(authService = authService, onShowInfo = { flow = LoginFlow.ONBOARDING })
        }
    }
}

@Composable
private fun Onboarding(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == slides.size - 1
    Column(modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Skip") }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = Theme.Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(slide.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.size(Theme.Spacing.md))
                Text(
                    slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Button(
            onClick = {
                if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (isLast) "Get Started" else "Next") }
    }
}

@Composable
private fun SignIn(authService: AuthService, onShowInfo: () -> Unit) {
    val store = remember { LoginStore() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val state by authService.state.collectAsState()
    val errorMessage by authService.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()

    store.email = email
    store.password = password
    val isAuthenticating = state is AuthState.Authenticating

    Column(
        modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onShowInfo) { Text("Info") }
        }
        Spacer(Modifier.weight(1f))
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(io.rocketpartners.hris.R.drawable.hris_logo),
            contentDescription = "HRIS logo",
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.size(Theme.Spacing.lg))
        Text("Welcome Back", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Sign in to manage your workspace",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(Theme.Spacing.xl))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; authService.clearError() },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                singleLine = true,
                isError = store.hasEmailInput && !store.isEmailValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(Theme.Spacing.md))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; authService.clearError() },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            if (errorMessage != null) {
                Spacer(Modifier.size(Theme.Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = Theme.Spacing.sm),
                    )
                }
            }

            Spacer(Modifier.size(Theme.Spacing.lg))
            Button(
                onClick = { scope.launch { authService.login(store.trimmedEmail, password) } },
                enabled = store.canSubmit && !isAuthenticating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Sign In", modifier = Modifier.padding(start = Theme.Spacing.sm))
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
