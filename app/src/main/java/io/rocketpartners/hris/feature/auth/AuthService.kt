package io.rocketpartners.hris.feature.auth

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Auth gating state RootScreen switches on. Mirrors iOS `AuthService.State`. */
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Authenticating : AuthState
    data class Authenticated(val user: User) : AuthState
}

/**
 * Owns the app's authentication state. Exposes [state] + [errorMessage] as [StateFlow]s (the Compose
 * counterpart to iOS's `@Observable`); the UI calls the suspend operations from its own coroutines.
 * Mirrors iOS `AuthService`.
 */
class AuthService(private val repository: AuthRepository) {

    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Attempts to restore a session at launch using a stored token. */
    suspend fun bootstrap() {
        _errorMessage.value = null
        _state.value = try {
            AuthState.Authenticated(repository.currentUser())
        } catch (_: Exception) {
            AuthState.Unauthenticated
        }
    }

    suspend fun login(email: String, password: String) {
        _errorMessage.value = null
        _state.value = AuthState.Authenticating
        try {
            val user = repository.login(email, password)
            _state.value = AuthState.Authenticated(user)
        } catch (e: Exception) {
            _errorMessage.value = (e as? ApiError)?.userMessage ?: "Sign in failed."
            _state.value = AuthState.Unauthenticated
        }
    }

    suspend fun logout() {
        runCatching { repository.logout() }
        _state.value = AuthState.Unauthenticated
    }

    /** Clears a stale sign-in error, e.g. once the user edits the form to retry. */
    fun clearError() {
        _errorMessage.value = null
    }
}
