package io.rocketpartners.hris.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The signed-in user from `GET /auth/me`.
 *
 * The backend returns `firstName`/`lastName` separately (no `name`); [name] is composed at decode
 * time, falling back to a direct `name` field or the email. [permissions] are authority strings
 * (e.g. "WFH_APPROVE") driving client-side UI gating only — the backend remains the authority.
 * Absent on older payloads → empty. Mirrors iOS `User`.
 */
@Serializable(with = UserSerializer::class)
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val permissions: List<String> = emptyList(),
) {
    /** Whether this user can act on WFH approvals (manager or admin). */
    val canApproveWfh: Boolean
        get() = "WFH_APPROVE" in permissions || "WFH_APPROVE_ALL" in permissions

    /** Whether this user can act on leave approvals (manager or HR). */
    val canApproveLeave: Boolean
        get() = "LEAVE_APPLICATION_APPROVE" in permissions
}

/** Raw wire shape backing [UserSerializer]. */
@Serializable
private data class UserWire(
    val id: Int,
    val email: String,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val permissions: List<String>? = null,
)

/** Decodes/encodes [User], composing a display name from `firstName`/`lastName` when needed. */
object UserSerializer : KSerializer<User> {
    override val descriptor: SerialDescriptor = UserWire.serializer().descriptor

    override fun deserialize(decoder: Decoder): User {
        val w = decoder.decodeSerializableValue(UserWire.serializer())
        val name = if (!w.name.isNullOrEmpty()) {
            w.name
        } else {
            val full = "${w.firstName.orEmpty()} ${w.lastName.orEmpty()}".trim()
            full.ifEmpty { w.email }
        }
        return User(id = w.id, name = name, email = w.email, permissions = w.permissions ?: emptyList())
    }

    override fun serialize(encoder: Encoder, value: User) {
        encoder.encodeSerializableValue(
            UserWire.serializer(),
            UserWire(id = value.id, email = value.email, name = value.name, permissions = value.permissions),
        )
    }
}
