package io.rocketpartners.hris.feature.notifications

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationUiState(
    val phase: Phase = Phase.Idle,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
)

/** Notification inbox with optimistic mark-read. Mirrors iOS `NotificationStore`. */
class NotificationStore(private val repository: NotificationRepository) {
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        val items = try {
            repository.list()
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load notifications."))) }
            return
        }
        val count = runCatching { repository.unreadCount() }.getOrNull() ?: items.count { !it.isRead }
        _state.update { it.copy(notifications = items, unreadCount = count, phase = Phase.Loaded) }
    }

    suspend fun refreshUnreadCount() {
        runCatching { repository.unreadCount() }.getOrNull()?.let { c -> _state.update { it.copy(unreadCount = c) } }
    }

    suspend fun markRead(notification: AppNotification) {
        if (notification.isRead) return
        _state.update { s ->
            s.copy(
                notifications = s.notifications.map { if (it.id == notification.id) it.copy(isRead = true) else it },
                unreadCount = maxOf(0, s.unreadCount - 1),
            )
        }
        runCatching { repository.markRead(notification.id) }
    }

    suspend fun markAllRead() {
        val current = _state.value
        if (current.unreadCount == 0 && current.notifications.none { !it.isRead }) return
        _state.update { s -> s.copy(notifications = s.notifications.map { it.copy(isRead = true) }, unreadCount = 0) }
        try {
            repository.markAllRead()
        } catch (e: Exception) {
            _state.update { it.copy(notifications = current.notifications, unreadCount = current.unreadCount, phase = Phase.Failed(errorMessage(e, "Couldn't mark all read."))) }
        }
    }
}
