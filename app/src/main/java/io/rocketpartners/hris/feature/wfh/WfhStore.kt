package io.rocketpartners.hris.feature.wfh

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.WfhSchedule
import io.rocketpartners.hris.model.WfhWeeklyUsage
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WfhUiState(
    val phase: Phase = Phase.Idle,
    val schedules: List<WfhSchedule> = emptyList(),
    val usage: WfhWeeklyUsage? = null,
)

/** Owns the employee's WFH days for the month + weekly usage. Mirrors iOS `WfhStore`. */
class WfhStore(
    private val repository: WfhRepository,
    private val month: YearMonth = YearMonth.now(),
) {
    private val _state = MutableStateFlow(WfhUiState())
    val state: StateFlow<WfhUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        val schedules = try {
            repository.schedules(month)
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load WFH."))) }
            return
        }
        val usage = runCatching { repository.weeklyUsage() }.getOrNull()
        _state.update { it.copy(schedules = schedules, usage = usage, phase = Phase.Loaded) }
    }

    /** Returns true on success so the caller can dismiss the sheet. */
    suspend fun scheduleDay(date: LocalDate, reason: String?): Boolean = try {
        repository.schedule(listOf(date), reason)
        load()
        true
    } catch (e: Exception) {
        _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't schedule WFH."))) }
        false
    }

    suspend fun cancel(schedule: WfhSchedule) {
        try {
            repository.cancel(schedule.id)
            load()
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't cancel WFH."))) }
        }
    }
}
