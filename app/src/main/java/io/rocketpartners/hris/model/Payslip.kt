package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A payslip from `GET /payslips/me`. `@JsonInclude(NON_NULL)` → most fields optional. The server's
 * relative `downloadUrl` is deliberately NOT decoded — the download URL is built from [id] against
 * the app's base URL instead. Mirrors iOS `Payslip`.
 */
@Serializable
data class Payslip(
    val id: Int,
    val payPeriodId: Int? = null,
    val payPeriodLabel: String? = null,
    /** `yyyy-MM-dd`. */
    val cutoffDate: String? = null,
    val fileName: String? = null,
    val contentType: String? = null,
    /** File size in bytes. */
    val fileSize: Int? = null,
    /** `UPLOAD` | `GENERATED`. */
    val source: String? = null,
    val createdAt: String? = null,
) {
    val parsedCutoff: Instant? get() = cutoffDate?.let(WireDate::parse)

    /** Human file size, e.g. "47 KB"; null when the size is absent. */
    val formattedSize: String? get() = fileSize?.let { formatFileSize(it.toLong()) }
}
