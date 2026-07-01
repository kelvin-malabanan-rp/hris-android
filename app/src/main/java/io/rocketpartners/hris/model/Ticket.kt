package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A support ticket from `GET /tickets` (list) and the ticket-level fields of `GET /tickets/{id}`.
 * Backend uses `@JsonInclude(NON_NULL)` → most fields optional. Mirrors iOS `Ticket`.
 */
@Serializable
data class Ticket(
    val id: Int,
    val subject: String? = null,
    val description: String? = null,
    /** `bug | feature | question | other` (default `other`). */
    val category: String? = null,
    /** `low | medium | high | critical` (default `medium`). */
    val priority: String? = null,
    /** `open | in_progress | resolved | closed`. */
    val status: String? = null,
    val userId: Int? = null,
    val userName: String? = null,
    /** Present on the list (`TicketResponse`) only; the detail response omits it. */
    val messageCount: Int? = null,
    /** Non-null ⇒ the ticket has been resolved/closed. */
    val resolvedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val parsedUpdatedAt: Instant? get() = updatedAt?.let(WireDate::parse)
    val parsedCreatedAt: Instant? get() = createdAt?.let(WireDate::parse)

    /**
     * True once the ticket has a [resolvedAt], or its status reads resolved/closed. Used to gate the
     * reply composer (closed tickets shouldn't accept new messages from the employee).
     */
    val isResolved: Boolean
        get() {
            if (resolvedAt != null) return true
            val s = (status ?: "").lowercase()
            return s == "resolved" || s == "closed"
        }

    /** Human label for the status pill (e.g. `in_progress` → "In Progress"). */
    val statusLabel: String get() = humanStatusLabel(status ?: "open")

    /** Title-cased priority label (e.g. `high` → "High"). */
    val priorityLabel: String?
        get() = priority?.takeIf { it.isNotEmpty() }?.let { it.lowercase().replaceFirstChar(Char::uppercase) }

    /** Title-cased category label (e.g. `feature` → "Feature"). */
    val categoryLabel: String?
        get() = category?.takeIf { it.isNotEmpty() }?.let { it.lowercase().replaceFirstChar(Char::uppercase) }
}

/**
 * One message in a ticket thread (`TicketMessageResponse`). Rendered by [isSupport] (support on one
 * side, the employee on the other) — never by comparing [userId].
 */
@Serializable
data class TicketMessage(
    val id: Int,
    val userId: Int? = null,
    val userName: String? = null,
    val userProfilePicture: String? = null,
    /** True ⇒ posted by support staff (render on the "other" side of the thread). */
    val isSupport: Boolean = false,
    val message: String? = null,
    val attachments: List<TicketAttachment> = emptyList(),
    val createdAt: String? = null,
) {
    val parsedCreatedAt: Instant? get() = createdAt?.let(WireDate::parse)
}

/**
 * A ticket/message attachment (`TicketAttachmentResponse`). [downloadUrl] is relative and served
 * behind the bearer-authed `GET /files` endpoints.
 */
@Serializable
data class TicketAttachment(
    val id: Int,
    val fileName: String? = null,
    val storedPath: String? = null,
    val contentType: String? = null,
    /** File size in bytes. */
    val fileSize: Int? = null,
    /** Relative download path (under `/files`). */
    val downloadUrl: String? = null,
    val createdAt: String? = null,
) {
    /** Whether this attachment is an image (drives inline preview vs. a generic file chip). */
    val isImage: Boolean get() = (contentType ?: "").lowercase().startsWith("image/")

    /** Human file size, e.g. "47 KB"; null when the size is absent. */
    val formattedSize: String? get() = fileSize?.let { formatFileSize(it.toLong()) }
}

/**
 * The full thread for `GET /tickets/{id}`: the ticket plus its messages and ticket-level
 * attachments. The ticket fields are flattened onto the same object as `messages`/`attachments`.
 */
@Serializable(with = TicketDetailSerializer::class)
data class TicketDetail(
    val ticket: Ticket,
    val messages: List<TicketMessage> = emptyList(),
    val attachments: List<TicketAttachment> = emptyList(),
) {
    val id: Int get() = ticket.id
}

/** Flattened wire shape: all [Ticket] fields plus the two thread arrays. */
@Serializable
private data class TicketDetailWire(
    val id: Int,
    val subject: String? = null,
    val description: String? = null,
    val category: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val userId: Int? = null,
    val userName: String? = null,
    val messageCount: Int? = null,
    val resolvedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val messages: List<TicketMessage> = emptyList(),
    val attachments: List<TicketAttachment> = emptyList(),
)

/** Decodes [TicketDetail] from the flattened wire object. Serialization is decode-focused. */
object TicketDetailSerializer : KSerializer<TicketDetail> {
    override val descriptor: SerialDescriptor = TicketDetailWire.serializer().descriptor

    override fun deserialize(decoder: Decoder): TicketDetail {
        val w = decoder.decodeSerializableValue(TicketDetailWire.serializer())
        val ticket = Ticket(
            id = w.id, subject = w.subject, description = w.description, category = w.category,
            priority = w.priority, status = w.status, userId = w.userId, userName = w.userName,
            messageCount = w.messageCount, resolvedAt = w.resolvedAt, createdAt = w.createdAt,
            updatedAt = w.updatedAt,
        )
        return TicketDetail(ticket = ticket, messages = w.messages, attachments = w.attachments)
    }

    override fun serialize(encoder: Encoder, value: TicketDetail) {
        val t = value.ticket
        encoder.encodeSerializableValue(
            TicketDetailWire.serializer(),
            TicketDetailWire(
                id = t.id, subject = t.subject, description = t.description, category = t.category,
                priority = t.priority, status = t.status, userId = t.userId, userName = t.userName,
                messageCount = t.messageCount, resolvedAt = t.resolvedAt, createdAt = t.createdAt,
                updatedAt = t.updatedAt, messages = value.messages, attachments = value.attachments,
            ),
        )
    }
}

/** Allowed category values for the new-ticket form picker; values are the lowercase wire strings. */
enum class TicketCategory(val wire: String) {
    BUG("bug"), FEATURE("feature"), QUESTION("question"), OTHER("other");

    val label: String get() = wire.replaceFirstChar(Char::uppercase)

    companion object {
        val DEFAULT = OTHER
    }
}

enum class TicketPriority(val wire: String) {
    LOW("low"), MEDIUM("medium"), HIGH("high"), CRITICAL("critical");

    val label: String get() = wire.replaceFirstChar(Char::uppercase)

    companion object {
        val DEFAULT = MEDIUM
    }
}

/** Draft used to create a ticket. Maps directly to the `ticket` JSON part. */
data class NewTicket(
    val subject: String = "",
    val description: String = "",
    val category: TicketCategory = TicketCategory.DEFAULT,
    val priority: TicketPriority = TicketPriority.DEFAULT,
)

/**
 * A file the user is attaching (JPEG bytes for photos, raw bytes for a picked document). Carries the
 * validated filename + MIME type for the multipart `files` part. Equality is by [id] (stable UUID),
 * since [data] is a large byte buffer.
 */
class UploadFile(
    val filename: String,
    val mimeType: String,
    val data: ByteArray,
    val id: UUID = UUID.randomUUID(),
) {
    val isWithinSizeLimit: Boolean get() = data.size <= MAX_BYTES

    /** Human file size, e.g. "1.2 MB". */
    val formattedSize: String get() = formatFileSize(data.size.toLong())

    override fun equals(other: Any?): Boolean = other is UploadFile && other.id == id
    override fun hashCode(): Int = id.hashCode()

    companion object {
        /** Max attachment size (10 MB) the backend enforces; checked client-side before upload. */
        const val MAX_BYTES = 10 * 1024 * 1024

        /** Allowed extensions the backend accepts (magic-byte validated server-side). */
        val ALLOWED_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png", "doc", "docx")
    }
}
