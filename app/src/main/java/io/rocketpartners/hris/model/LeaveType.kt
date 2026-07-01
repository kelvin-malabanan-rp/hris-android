package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/** A selectable leave type from `/leave-types/active`. Mirrors iOS `LeaveType`. */
@Serializable
data class LeaveType(
    val id: Int,
    val name: String,
    val code: String? = null,
    val color: String? = null,
    /** Whether this type advises a medical certificate (only Sick Leave, server-side). */
    val requiresMedicalCert: Boolean? = null,
    /** Day threshold above which the cert is advised; null ⇒ always (when [requiresMedicalCert]). */
    val medicalCertDaysThreshold: Int? = null,
) {
    /**
     * Whether a request of [days] for this type should advise attaching a medical certificate.
     * Advisory only — the backend does NOT enforce it on submit. A null threshold ⇒ always required
     * (when [requiresMedicalCert] is true); otherwise required only when `days > threshold`.
     */
    fun requiresMedicalCertificate(days: Double): Boolean {
        if (requiresMedicalCert != true) return false
        val threshold = medicalCertDaysThreshold ?: return true
        return days > threshold.toDouble()
    }
}
