package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/**
 * The current user's profile from `GET /users/me` (a focused subset of the backend's UserResponse).
 * Address + emergency-contact fields are flat top-level strings (no nested object) and may be
 * absent. Mirrors iOS `UserProfile`.
 */
@Serializable
data class UserProfile(
    val id: Int,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val phone: String? = null,
    val personalEmail: String? = null,
    val employeeId: String? = null,
    val departmentName: String? = null,
    val positionTitle: String? = null,
    val profileImageUrl: String? = null,
    // Address (flat fields).
    val address: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    // Emergency contact — mobile is separate from phone.
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val emergencyContactMobile: String? = null,
    val emergencyContactRelationship: String? = null,
) {
    val displayName: String
        get() {
            fullName?.takeIf { it.isNotEmpty() }?.let { return it }
            val parts = listOfNotNull(firstName, lastName).filter { it.isNotEmpty() }
            return if (parts.isEmpty()) (email ?: "—") else parts.joinToString(" ")
        }

    /**
     * A single-line address built from the non-empty flat fields (street, line 2, city, state,
     * postal code, country), or null if no address is on file.
     */
    val formattedAddress: String?
        get() {
            val parts = listOfNotNull(address, addressLine2, city, state, postalCode, country)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return if (parts.isEmpty()) null else parts.joinToString(", ")
        }

    /** Whether any emergency-contact field is populated (drives the read-only card's visibility). */
    val hasEmergencyContact: Boolean
        get() = listOfNotNull(
            emergencyContactName, emergencyContactPhone, emergencyContactMobile, emergencyContactRelationship,
        ).any { it.trim().isNotEmpty() }
}
