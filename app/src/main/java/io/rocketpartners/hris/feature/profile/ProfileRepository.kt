package io.rocketpartners.hris.feature.profile

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Editable profile fields. Null fields are omitted from the PATCH body (the backend PATCH is
 * null-ignore — a null leaves the value unchanged, it cannot clear it). Mirrors iOS `ProfileUpdate`.
 */
data class ProfileUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val personalEmail: String? = null,
    val address: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactMobile: String? = null,
    val emergencyContactRelationship: String? = null,
)

interface ProfileRepository {
    suspend fun profile(): UserProfile
    suspend fun updateProfile(update: ProfileUpdate): UserProfile
    suspend fun changePassword(current: String, new: String, confirm: String)
}

class LiveProfileRepository(private val client: ApiClient) : ProfileRepository {

    @Serializable
    private data class UpdateBody(
        val firstName: String?,
        val lastName: String?,
        val phone: String?,
        val personalEmail: String?,
        val address: String?,
        val addressLine2: String?,
        val city: String?,
        val state: String?,
        val postalCode: String?,
        val country: String?,
        val emergencyContactName: String?,
        val emergencyContactPhone: String?,
        val emergencyContactMobile: String?,
        val emergencyContactRelationship: String?,
    )

    @Serializable
    private data class PasswordBody(
        val currentPassword: String,
        val newPassword: String,
        val confirmPassword: String,
    )

    override suspend fun profile(): UserProfile = client.send(Endpoint("users/me"))

    override suspend fun updateProfile(update: ProfileUpdate): UserProfile {
        // AppJson has explicitNulls = false, so null fields are omitted → partial PATCH.
        val body = AppJson.encodeToString(
            UpdateBody(
                update.firstName, update.lastName, update.phone, update.personalEmail,
                update.address, update.addressLine2, update.city, update.state,
                update.postalCode, update.country, update.emergencyContactName,
                update.emergencyContactPhone, update.emergencyContactMobile,
                update.emergencyContactRelationship,
            ),
        ).encodeToByteArray()
        return client.send(Endpoint("users/me", Endpoint.Method.PATCH, body = body))
    }

    override suspend fun changePassword(current: String, new: String, confirm: String) {
        val body = AppJson.encodeToString(PasswordBody(current, new, confirm)).encodeToByteArray()
        client.sendVoid(Endpoint("users/me/password", Endpoint.Method.PUT, body = body))
    }
}
