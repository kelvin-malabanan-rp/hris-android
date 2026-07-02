package io.rocketpartners.hris.feature.wfh

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.WfhSchedule
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

data class WfhApprovalsUiState(
    val phase: Phase = Phase.Idle,
    val pending: List<WfhSchedule> = emptyList(),
    val actionError: String? = null,
    val undoMessage: String? = null,
)

/**
 * Manager WFH approval inbox with the same delayed-commit + undo behavior as the leave inbox.
 * Mirrors iOS `WfhApprovalsStore`.
 */
class WfhApprovalsStore(
    private val repository: WfhRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val undoWindowMs: Long = 4000,
) {
    private val _state = MutableStateFlow(WfhApprovalsUiState())
    val state: StateFlow<WfhApprovalsUiState> = _state.asStateFlow()

    private class Staged(val schedule: WfhSchedule, val index: Int, val isApprove: Boolean)

    private val staged = mutableMapOf<Int, Staged>()
    private val commits = mutableMapOf<Int, Job>()
    private var undoTargetId: Int? = null

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(pending = repository.pendingApprovals(), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load approvals."))) }
        }
    }

    fun approve(schedule: WfhSchedule) = stage(schedule, isApprove = true, message = "Approved")
    fun reject(schedule: WfhSchedule) = stage(schedule, isApprove = false, message = "Rejected")

    private fun stage(schedule: WfhSchedule, isApprove: Boolean, message: String) {
        val index = _state.value.pending.indexOfFirst { it.id == schedule.id }
        if (index < 0) return
        staged[schedule.id] = Staged(schedule, index, isApprove)
        undoTargetId = schedule.id
        _state.update { it.copy(pending = it.pending.filterNot { p -> p.id == schedule.id }, undoMessage = message) }
        commits[schedule.id] = scope.launch {
            delay(undoWindowMs)
            commit(schedule.id)
        }
    }

    private suspend fun commit(id: Int) {
        commits.remove(id)
        val item = staged.remove(id) ?: return
        if (undoTargetId == id) clearUndoToast()
        try {
            if (item.isApprove) repository.approve(item.schedule.id, null) else repository.reject(item.schedule.id, null)
        } catch (e: Exception) {
            _state.update { s -> s.copy(pending = reinsert(s.pending, item.schedule, item.index), actionError = errorMessage(e, "Couldn't update the request.")) }
        }
    }

    fun undo() {
        val id = undoTargetId ?: return
        commits.remove(id)?.cancel()
        staged.remove(id)?.let { item ->
            _state.update { s -> s.copy(pending = reinsert(s.pending, item.schedule, item.index)) }
        }
        clearUndoToast()
    }

    fun clearActionError() = _state.update { it.copy(actionError = null) }

    private fun clearUndoToast() {
        undoTargetId = null
        _state.update { it.copy(undoMessage = null) }
    }

    suspend fun waitForPendingCommits() { commits.values.toList().forEach { it.join() } }

    private fun reinsert(list: List<WfhSchedule>, item: WfhSchedule, index: Int): List<WfhSchedule> =
        list.toMutableList().apply { add(minOf(index, size), item) }
}
