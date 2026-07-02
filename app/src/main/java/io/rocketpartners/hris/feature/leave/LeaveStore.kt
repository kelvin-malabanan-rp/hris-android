package io.rocketpartners.hris.feature.leave

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveBalance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LeaveUiState(
    val phase: Phase = Phase.Idle,
    val leaves: List<LeaveApplication> = emptyList(),
    val balances: List<LeaveBalance> = emptyList(),
)

/** Owns the employee's leave list + balances. Mirrors iOS `LeaveStore`. */
class LeaveStore(private val repository: LeaveRepository) {
    private val _state = MutableStateFlow(LeaveUiState())
    val state: StateFlow<LeaveUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        val leaves = try {
            repository.myLeaves()
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load leave."))) }
            return
        }
        // Balances are secondary — degrade gracefully if they fail.
        val balances = runCatching { repository.balances() }.getOrDefault(emptyList())
        _state.update { it.copy(leaves = leaves, balances = balances, phase = Phase.Loaded) }
    }

    suspend fun cancel(leave: LeaveApplication) = replace(leave.id) { repository.cancel(leave.id) }

    suspend fun requestCancellation(leave: LeaveApplication, reason: String) =
        replace(leave.id) { repository.requestCancellation(leave.id, reason) }

    /** Replaces an edited request in place after a successful edit. */
    fun applyEdit(updated: LeaveApplication) {
        _state.update { s -> s.copy(leaves = s.leaves.map { if (it.id == updated.id) updated else it }) }
    }

    private suspend fun replace(id: Int, action: suspend () -> LeaveApplication) {
        try {
            val updated = action()
            _state.update { s -> s.copy(leaves = s.leaves.map { if (it.id == id) updated else it }) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Action failed."))) }
        }
    }
}
