package io.rocketpartners.hris.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import io.rocketpartners.hris.R
import io.rocketpartners.hris.designsystem.GlassCard
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.auth.AuthService
import io.rocketpartners.hris.feature.auth.AuthState
import kotlinx.coroutines.launch

private enum class LoginFlow { ONBOARDING, SIGN_IN }

private data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
)

private val slides = listOf(
    OnboardingSlide("Smart Calendar", "Stay in sync with your team. Track schedules, meetings, and team availability in one unified calendar.", Icons.Filled.CalendarMonth, Theme.Accent.INFO.tint),
    OnboardingSlide("Seamless Leaves", "Apply for leaves with just a few taps. Get instant approval updates and track your balance in real time.", Icons.Filled.FlightTakeoff, Theme.Accent.LEAVE.tint),
    OnboardingSlide("Flexible WFH", "Coordinate your work-from-home days. Log your location and sync with your team shifts seamlessly.", Icons.Filled.Home, Theme.Accent.PENDING.tint),
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
        // Header: HRIS logo + name (left), Skip pill (right).
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.hris_logo), contentDescription = null, modifier = Modifier.size(28.dp))
            Text("HRIS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = Theme.Spacing.sm))
            Spacer(Modifier.weight(1f))
            Pill("Skip", onClick = onFinish)
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = Theme.Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.size(180.dp).background(slide.accent.copy(alpha = Theme.Opacity.fill), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(slide.icon, contentDescription = null, tint = slide.accent, modifier = Modifier.size(64.dp))
                }
                Spacer(Modifier.size(Theme.Spacing.xxl))
                Text(slide.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.size(Theme.Spacing.md))
                Text(
                    slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        PageDots(count = slides.size, active = pagerState.currentPage, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = Theme.Spacing.lg))

        Button(
            onClick = {
                if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(if (isLast) Icons.Filled.AutoAwesome else Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(if (isLast) "Get Started" else "Next", modifier = Modifier.padding(start = Theme.Spacing.sm))
        }
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.sm),
    )
}

@Composable
private fun PageDots(count: Int, active: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
            .padding(horizontal = Theme.Spacing.md, vertical = Theme.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == active) 10.dp else 8.dp)
                    .background(
                        if (i == active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        CircleShape,
                    ),
            )
        }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .clickable(onClick = onShowInfo)
                    .padding(horizontal = Theme.Spacing.md, vertical = Theme.Spacing.sm),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(18.dp))
                Text("Info", style = MaterialTheme.typography.labelLarge, color = Theme.brand)
            }
        }
        Spacer(Modifier.weight(1f))
        Image(painterResource(R.drawable.hris_logo), contentDescription = "HRIS logo", modifier = Modifier.size(72.dp))
        Spacer(Modifier.size(Theme.Spacing.lg))
        Text("Welcome Back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Sign in to manage your workspace", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1.4f))

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
    }
}
