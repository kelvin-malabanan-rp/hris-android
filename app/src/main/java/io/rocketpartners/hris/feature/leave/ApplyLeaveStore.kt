package io.rocketpartners.hris.feature.leave

import io.rocketpartners.hris.core.networking.WireDate
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.LeaveApplication
import io.rocketpartners.hris.model.LeaveBalance
import io.rocketpartners.hris.model.LeaveType
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Phase for the apply/edit form. Mirrors iOS `ApplyLeaveStore.Phase`. */
sealed interface ApplyPhase {
    data object Idle : ApplyPhase
    data object Submitting : ApplyPhase
    data object Submitted : ApplyPhase
    data class Failed(val message: String) : ApplyPhase
}

data class ApplyLeaveUiState(
    val phase: ApplyPhase = ApplyPhase.Idle,
    val leaveTypes: List<LeaveType> = emptyList(),
    val balances: List<LeaveBalance> = emptyList(),
    val typesUnavailable: Boolean = false,
    val selectedTypeId: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String = "",
    val typeLocked: Boolean = false,
    val lockedTypeName: String? = null,
) {
    val selectedType: LeaveType? get() = leaveTypes.firstOrNull { it.id == selectedTypeId }
    val selectedBalance: LeaveBalance? get() = balances.firstOrNull { it.leaveTypeId == selectedTypeId }

    val canSubmit: Boolean
        get() = selectedTypeId != null && !endDate.isBefore(startDate) && phase != ApplyPhase.Submitting

    /** Inclusive whole-day count of the requested range (client-side preview). Zero if invalid. */
    val requestedDays: Int
        get() = if (endDate.isBefore(startDate)) 0 else (ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1)

    /** Projected remaining balance after this request (null if no balance row). */
    val projectedRemaining: Double? get() = selectedBalance?.remainingDays?.let { it - requestedDays }

    /** Whether the request exceeds the available balance (mirrors the server's 400 backstop). */
    val exceedsBalance: Boolean
        get() {
            val balance = selectedBalance ?: return false
            val total = balance.totalDays ?: return false
            val remaining = balance.remainingDays ?: return false
            return total > 0 && requestedDays > remaining
        }

    /** Advisory sentence when the selected type + duration triggers the medical-cert rule, else null. */
    val medicalCertAdvisory: String?
        get() {
            val type = selectedType ?: return null
            if (!type.requiresMedicalCertificate(requestedDays.toDouble())) return null
            val threshold = type.medicalCertDaysThreshold
                ?: return "${type.name} requires a medical certificate — please submit it to HR."
            val dayWord = if (threshold == 1) "day" else "days"
            return "${type.name} over $threshold $dayWord requires a medical certificate — please submit it to HR."
        }
}

/** Drives the apply/edit leave form. Mirrors iOS `ApplyLeaveStore`. */
class ApplyLeaveStore(
    private val repository: LeaveRepository,
    editing: LeaveApplication? = null,
    today: LocalDate = LocalDate.now(),
) {
    val editingId: Int? = editing?.id

    private val _state = MutableStateFlow(
        run {
            fun parse(s: String?) = s?.let(WireDate::parse)?.let { LocalDate.ofInstant(it, ZoneOffset.UTC) }
            ApplyLeaveUiState(
                selectedTypeId = editing?.leaveTypeId,
                startDate = parse(editing?.startDate) ?: today,
                endDate = parse(editing?.endDate) ?: today,
                reason = editing?.reason ?: "",
                typeLocked = editing != null,
                lockedTypeName = editing?.leaveTypeName,
            )
        },
    )
    val state: StateFlow<ApplyLeaveUiState> = _state.asStateFlow()

    var savedApplication: LeaveApplication? = null
        private set

    fun setType(id: Int?) = _state.update { it.copy(selectedTypeId = id) }
    fun setStart(date: LocalDate) = _state.update { it.copy(startDate = date, endDate = if (it.endDate.isBefore(date)) date else it.endDate) }
    fun setEnd(date: LocalDate) = _state.update { it.copy(endDate = date) }
    fun setReason(text: String) = _state.update { it.copy(reason = text) }

    suspend fun loadTypes() {
        val types = runCatching { repository.activeLeaveTypes() }.getOrDefault(emptyList())
        val balances = runCatching { repository.balances() }.getOrDefault(emptyList())
        _state.update {
            it.copy(
                leaveTypes = types,
                typesUnavailable = types.isEmpty(),
                balances = balances,
                selectedTypeId = it.selectedTypeId ?: types.firstOrNull()?.id,
            )
        }
    }

    /** Returns true on success. Routes to PUT (edit) or POST (create). */
    suspend fun submit(): Boolean {
        val current = _state.value
        val typeId = current.selectedTypeId ?: return false
        _state.update { it.copy(phase = ApplyPhase.Submitting) }
        val draft = NewLeaveApplication(typeId, current.startDate, current.endDate, current.reason.ifEmpty { null })
        return try {
            val result = editingId?.let { repository.edit(it, draft) } ?: repository.apply(draft)
            savedApplication = result
            _state.update { it.copy(phase = ApplyPhase.Submitted) }
            true
        } catch (e: Exception) {
            _state.update { it.copy(phase = ApplyPhase.Failed(errorMessage(e, "Couldn't submit leave."))) }
            false
        }
    }
}
