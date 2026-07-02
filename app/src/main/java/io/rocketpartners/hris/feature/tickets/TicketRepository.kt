package io.rocketpartners.hris.feature.tickets

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.MultipartFile
import io.rocketpartners.hris.core.networking.Paged
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.core.networking.sendMultipart
import io.rocketpartners.hris.model.NewTicket
import io.rocketpartners.hris.model.Ticket
import io.rocketpartners.hris.model.TicketDetail
import io.rocketpartners.hris.model.TicketMessage
import io.rocketpartners.hris.model.UploadFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Optional filters for the ticket list. All null ⇒ unfiltered (still self-scoped). Mirrors iOS. */
data class TicketFilters(
    val status: String? = null,
    val category: String? = null,
    val priority: String? = null,
    val search: String? = null,
)

interface TicketRepository {
    suspend fun myTickets(filters: TicketFilters = TicketFilters()): List<Ticket>
    suspend fun ticket(id: Int): TicketDetail
    suspend fun create(draft: NewTicket, attachments: List<UploadFile>): Ticket
    suspend fun reply(ticketId: Int, message: String, attachments: List<UploadFile>): TicketMessage
    suspend fun attachmentData(downloadUrl: String): ByteArray
}

class LiveTicketRepository(private val client: ApiClient) : TicketRepository {

    @Serializable private data class CreateBody(val subject: String, val description: String, val category: String, val priority: String)
    @Serializable private data class ReplyBody(val message: String)

    override suspend fun myTickets(filters: TicketFilters): List<Ticket> {
        val all = mutableListOf<Ticket>()
        var page = 0
        while (true) {
            val query = buildList {
                add("page" to "$page"); add("size" to "100")
                filters.status?.let { add("status" to it) }
                filters.category?.let { add("category" to it) }
                filters.priority?.let { add("priority" to it) }
                filters.search?.takeIf { it.isNotEmpty() }?.let { add("search" to it) }
            }
            val result: Paged<Ticket> = client.send(Endpoint("tickets", query = query), Paged.serializer(Ticket.serializer()))
            all.addAll(result.content)
            if (result.last != false || result.content.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun ticket(id: Int): TicketDetail = client.send(Endpoint("tickets/$id"))

    override suspend fun create(draft: NewTicket, attachments: List<UploadFile>): Ticket {
        val json = AppJson.encodeToString(
            CreateBody(draft.subject, draft.description, draft.category.wire, draft.priority.wire),
        ).encodeToByteArray()
        return client.sendMultipart(
            Endpoint("tickets", Endpoint.Method.POST),
            jsonPartName = "ticket", jsonData = json,
            filePartName = "files", files = attachments.map(::toMultipart),
        )
    }

    override suspend fun reply(ticketId: Int, message: String, attachments: List<UploadFile>): TicketMessage {
        val json = AppJson.encodeToString(ReplyBody(message)).encodeToByteArray()
        return client.sendMultipart(
            Endpoint("tickets/$ticketId/messages", Endpoint.Method.POST),
            jsonPartName = "message", jsonData = json,
            filePartName = "files", files = attachments.map(::toMultipart),
        )
    }

    override suspend fun attachmentData(downloadUrl: String): ByteArray {
        val path = if (downloadUrl.startsWith("/")) downloadUrl else "/$downloadUrl"
        return client.sendData(Endpoint(path))
    }

    private fun toMultipart(file: UploadFile) = MultipartFile(file.filename, file.mimeType, file.data)
}
