package io.rocketpartners.hris.feature.announcements

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.Announcement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AnnouncementsUiState(
    val phase: Phase = Phase.Idle,
    val announcements: List<Announcement> = emptyList(),
    val selectedCategory: String? = null,
)

/** Feed store for the announcements screen (pinned-first, category filter). Mirrors iOS. */
class AnnouncementsStore(private val repository: AnnouncementRepository) {
    private val _state = MutableStateFlow(AnnouncementsUiState())
    val state: StateFlow<AnnouncementsUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            val feed = repository.feed(page = 0, category = _state.value.selectedCategory, search = null)
            _state.update { it.copy(announcements = feed, phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load announcements."))) }
        }
    }

    suspend fun select(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
        load()
    }
}

data class AnnouncementDetailUiState(
    val phase: Phase = Phase.Idle,
    val announcement: Announcement? = null,
)

/** Detail store: seed from the feed row, then refresh from `/announcements/{id}`. Mirrors iOS. */
class AnnouncementDetailStore(
    private val id: Int,
    seed: Announcement?,
    private val repository: AnnouncementRepository,
) {
    private val _state = MutableStateFlow(AnnouncementDetailUiState(announcement = seed))
    val state: StateFlow<AnnouncementDetailUiState> = _state.asStateFlow()

    suspend fun load() {
        if (_state.value.announcement == null) _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(announcement = repository.detail(id), phase = Phase.Loaded) }
        } catch (e: Exception) {
            if (_state.value.announcement != null) {
                _state.update { it.copy(phase = Phase.Loaded) }
            } else {
                _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load this announcement."))) }
            }
        }
    }
}
