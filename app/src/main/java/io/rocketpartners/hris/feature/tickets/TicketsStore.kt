package io.rocketpartners.hris.feature.tickets

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.core.ui.errorMessage
import io.rocketpartners.hris.model.NewTicket
import io.rocketpartners.hris.model.Ticket
import io.rocketpartners.hris.model.TicketDetail
import io.rocketpartners.hris.model.UploadFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TicketsUiState(
    val phase: Phase = Phase.Idle,
    val tickets: List<Ticket> = emptyList(),
    val createError: String? = null,
)

/** Support-ticket list + create. Mirrors iOS `TicketsStore`. */
class TicketsStore(private val repository: TicketRepository) {
    private val _state = MutableStateFlow(TicketsUiState())
    val state: StateFlow<TicketsUiState> = _state.asStateFlow()

    suspend fun load() {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(tickets = repository.myTickets(), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load your tickets."))) }
        }
    }

    /** Returns true on success (view dismisses its sheet); prepends the new ticket. */
    suspend fun create(draft: NewTicket, attachments: List<UploadFile>): Boolean = try {
        val created = repository.create(draft, attachments)
        _state.update { it.copy(tickets = listOf(created) + it.tickets) }
        true
    } catch (e: Exception) {
        _state.update { it.copy(createError = errorMessage(e, "Couldn't create your ticket.")) }
        false
    }

    fun clearCreateError() = _state.update { it.copy(createError = null) }
}

data class TicketDetailUiState(
    val phase: Phase = Phase.Idle,
    val detail: TicketDetail? = null,
    val isSending: Boolean = false,
    val replyError: String? = null,
)

/** A single ticket thread + reply composer. Mirrors iOS `TicketDetailStore`. */
class TicketDetailStore(private val repository: TicketRepository) {
    private val _state = MutableStateFlow(TicketDetailUiState())
    val state: StateFlow<TicketDetailUiState> = _state.asStateFlow()

    suspend fun load(id: Int) {
        _state.update { it.copy(phase = Phase.Loading) }
        try {
            _state.update { it.copy(detail = repository.ticket(id), phase = Phase.Loaded) }
        } catch (e: Exception) {
            _state.update { it.copy(phase = Phase.Failed(errorMessage(e, "Couldn't load this ticket."))) }
        }
    }

    /** Posts a reply; appends the returned message. Returns true so the composer clears. */
    suspend fun reply(message: String, attachments: List<UploadFile>): Boolean {
        val detail = _state.value.detail ?: return false
        val trimmed = message.trim()
        if (trimmed.isEmpty() && attachments.isEmpty()) return false
        _state.update { it.copy(isSending = true) }
        return try {
            val posted = repository.reply(detail.ticket.id, trimmed, attachments)
            _state.update { it.copy(detail = detail.copy(messages = detail.messages + posted), isSending = false) }
            true
        } catch (e: Exception) {
            _state.update { it.copy(isSending = false, replyError = errorMessage(e, "Couldn't send your reply.")) }
            false
        }
    }

    fun clearReplyError() = _state.update { it.copy(replyError = null) }
}
