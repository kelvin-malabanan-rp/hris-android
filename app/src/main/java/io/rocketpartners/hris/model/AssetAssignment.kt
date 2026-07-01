package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable

/**
 * An asset currently checked out to the signed-in user, from `GET /asset-assignments/my-assets`.
 * Backend uses `@JsonInclude(NON_NULL)` → most fields optional. Only `CHECKED_OUT` rows are
 * returned, and the response is a bare array (not paged). Mirrors iOS `AssetAssignment`.
 */
@Serializable
data class AssetAssignment(
    val id: Int,
    val assetId: Int? = null,
    val assetName: String? = null,
    val assetTag: String? = null,
    val categoryName: String? = null,
    val quantityAssigned: Int? = null,
    /** `LocalDateTime` (server-local, no offset). */
    val checkedOutAt: String? = null,
    /** `yyyy-MM-dd`. */
    val expectedReturnDate: String? = null,
    /** One of `NEW | GOOD | FAIR | POOR | DAMAGED`. */
    val conditionOnCheckout: String? = null,
    val assignedByName: String? = null,
    val checkoutNotes: String? = null,
    val status: String? = null,
) {
    val parsedExpectedReturn: Instant? get() = expectedReturnDate?.let(WireDate::parse)

    /** Whether the return is overdue or coming up. Computed client-side (no server scheduler). */
    enum class ReturnState { NONE, DUE_SOON, OVERDUE }

    /**
     * Civil-day comparison of [expectedReturnDate] against [today]. Wire dates parse at UTC, so both
     * sides are reduced to UTC civil days to avoid timezone drift.
     */
    fun returnState(today: Instant, dueSoonWithinDays: Int = 7): ReturnState {
        val expected = parsedExpectedReturn ?: return ReturnState.NONE
        val days = ChronoUnit.DAYS.between(WireDate.civilDay(today), WireDate.civilDay(expected))
        return when {
            days < 0 -> ReturnState.OVERDUE
            days <= dueSoonWithinDays -> ReturnState.DUE_SOON
            else -> ReturnState.NONE
        }
    }
}
