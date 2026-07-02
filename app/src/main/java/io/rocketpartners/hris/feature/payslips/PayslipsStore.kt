package io.rocketpartners.hris.feature.payslips

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.Payslip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PayslipsUiState(
    val phase: Phase = Phase.Idle,
    val payslips: List<Payslip> = emptyList(),
    val selectedPeriod: String? = null,
    /** Ids currently downloading (drives per-row spinners). */
    val downloadingIds: Set<Int> = emptySet(),
    val downloadError: String? = null,
) {
    /** Distinct pay-period labels in load order (newest-first), for the filter control. */
    val periods: List<String>
        get() {
            val seen = LinkedHashSet<String>()
            payslips.mapNotNull { it.payPeriodLabel }.filter { it.isNotEmpty() }.forEach { seen.add(it) }
            return seen.toList()
        }

    /** Payslips after applying the selected-period filter. */
    val filtered: List<Payslip>
        get() = selectedPeriod?.let { period -> payslips.filter { it.payPeriodLabel == period } } ?: payslips
}

/** Owns the employee's payslips + PDF downloads. Mirrors iOS `PayslipsStore`. */
class PayslipsStore(private val repository: PayslipRepository) {
    private val _state = MutableStateFlow(PayslipsUiState())
    val state: StateFlow<PayslipsUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(payslips = repository.myPayslips(), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load your payslips."))) }
        }
    }

    fun selectPeriod(period: String?) = _state.update { it.copy(selectedPeriod = period) }

    /** Downloads a payslip's PDF bytes, tracking per-row state. Null on failure (sets error). */
    suspend fun download(payslip: Payslip): ByteArray? {
        _state.update { it.copy(downloadingIds = it.downloadingIds + payslip.id) }
        return try {
            repository.downloadData(payslip.id)
        } catch (e: Exception) {
            _state.update { it.copy(downloadError = errorMessage(e, "Couldn't open this payslip.")) }
            null
        } finally {
            _state.update { it.copy(downloadingIds = it.downloadingIds - payslip.id) }
        }
    }

    fun clearDownloadError() = _state.update { it.copy(downloadError = null) }

    /** Sanitized temp filename: prefix with id, strip path separators / parent-dir tokens. */
    fun safeFileName(payslip: Payslip): String {
        val name = (payslip.fileName ?: "payslip-${payslip.id}.pdf").replace("/", "-").replace("..", "-")
        return "${payslip.id}-$name"
    }
}
