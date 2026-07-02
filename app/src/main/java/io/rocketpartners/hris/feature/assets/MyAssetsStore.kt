package io.rocketpartners.hris.feature.assets

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.AssetAssignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MyAssetsUiState(
    val phase: Phase = Phase.Idle,
    val assets: List<AssetAssignment> = emptyList(),
)

/** Owns the signed-in user's checked-out assets. Mirrors iOS `MyAssetsStore`. */
class MyAssetsStore(private val repository: AssetRepository) {
    private val _state = MutableStateFlow(MyAssetsUiState())
    val state: StateFlow<MyAssetsUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(assets = repository.myAssets(), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load your assets."))) }
        }
    }
}
