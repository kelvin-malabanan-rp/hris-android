package io.rocketpartners.hris.feature.leave

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveApprovalStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaveApprovalsUiState(
    val phase: Phase = Phase.Idle,
    val pending: List<LeaveApplication> = emptyList(),
    val actionError: String? = null,
    /** Non-null while the most recent action can still be undone; drives the undo toast. */
    val undoMessage: String? = null,
)

/**
 * Approver leave inbox. Approve/reject use **delayed commit**: the row is removed optimistically and
 * an undo toast shown, but the (non-reversible) backend call fires only after [undoWindowMs] elapses.
 * Undo cancels the pending timer. Mirrors iOS `LeaveApprovalsStore`.
 */
class LeaveApprovalsStore(
    private val repository: LeaveRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val undoWindowMs: Long = 4000,
) {
    private val _state = MutableStateFlow(LeaveApprovalsUiState())
    val state: StateFlow<LeaveApprovalsUiState> = _state.asStateFlow()

    private sealed interface Decision {
        data class Stage(val stage: LeaveApprovalStage) : Decision
        data object Cancellation : Decision
    }

    private class Staged(val application: LeaveApplication, val index: Int, val decision: Decision, val isApprove: Boolean)

    private val staged = mutableMapOf<Int, Staged>()
    private val commits = mutableMapOf<Int, Job>()
    private var undoTargetId: Int? = null

    private fun decision(app: LeaveApplication): Decision? = when {
        app.isCancellationRequest -> Decision.Cancellation
        app.approvalStage != null -> Decision.Stage(app.approvalStage!!)
        else -> null
    }

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(pending = repository.pendingApprovals(), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load approvals."))) }
        }
    }

    fun approve(application: LeaveApplication) = stage(application, isApprove = true, message = "Approved")
    fun reject(application: LeaveApplication) = stage(application, isApprove = false, message = "Rejected")

    private fun stage(application: LeaveApplication, isApprove: Boolean, message: String) {
        val decision = decision(application) ?: run {
            _state.update { it.copy(actionError = "This request isn't awaiting your approval.") }
            return
        }
        val index = _state.value.pending.indexOfFirst { it.id == application.id }
        if (index < 0) return
        staged[application.id] = Staged(application, index, decision, isApprove)
        undoTargetId = application.id
        _state.update { it.copy(pending = it.pending.filterNot { p -> p.id == application.id }, undoMessage = message) }
        commits[application.id] = scope.launch {
            delay(undoWindowMs)
            commit(application.id)
        }
    }

    private suspend fun commit(id: Int) {
        commits.remove(id)
        val item = staged.remove(id) ?: return
        if (undoTargetId == id) clearUndoToast()
        try {
            when {
                item.isApprove && item.decision is Decision.Stage -> repository.approve(item.application.id, (item.decision as Decision.Stage).stage, null)
                !item.isApprove && item.decision is Decision.Stage -> repository.reject(item.application.id, (item.decision as Decision.Stage).stage, null)
                item.isApprove -> repository.approveCancellation(item.application.id, null)
                else -> repository.rejectCancellation(item.application.id, null)
            }
        } catch (e: Exception) {
            _state.update { s -> s.copy(pending = reinsert(s.pending, item.application, item.index), actionError = errorMessage(e, "Couldn't update the request.")) }
        }
    }

    fun undo() {
        val id = undoTargetId ?: return
        commits.remove(id)?.cancel()
        staged.remove(id)?.let { item ->
            _state.update { s -> s.copy(pending = reinsert(s.pending, item.application, item.index)) }
        }
        clearUndoToast()
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }

    private fun clearUndoToast() {
        undoTargetId = null
        _state.update { it.copy(undoMessage = null) }
    }

    /** Test hook: awaits all in-flight commit timers. */
    suspend fun waitForPendingCommits() { commits.values.toList().forEach { it.join() } }

    private fun reinsert(list: List<LeaveApplication>, item: LeaveApplication, index: Int): List<LeaveApplication> =
        list.toMutableList().apply { add(minOf(index, size), item) }
}
