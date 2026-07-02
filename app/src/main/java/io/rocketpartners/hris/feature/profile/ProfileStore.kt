package io.rocketpartners.hris.feature.profile

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ProfileUiState(
    val phase: Phase = Phase.Idle,
    val profile: UserProfile? = null,
)

/** Owns the current user's profile + edit/password mutations. Mirrors iOS `ProfileStore`. */
class ProfileStore(private val repository: ProfileRepository) {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(profile = repository.profile(), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load profile."))) }
        }
    }

    /** Returns null on success, or a user-facing error message (leaves [Phase] untouched). */
    suspend fun save(update: ProfileUpdate): String? = try {
        val updated = repository.updateProfile(update)
        _state.update { it.copy(profile = updated) }
        null
    } catch (e: Exception) {
        errorMessage(e, "Couldn't update profile.")
    }

    /** Returns null on success, or a user-facing error message. */
    suspend fun changePassword(current: String, new: String, confirm: String): String? = try {
        repository.changePassword(current, new, confirm)
        null
    } catch (e: Exception) {
        errorMessage(e, "Couldn't change password.")
    }
}
