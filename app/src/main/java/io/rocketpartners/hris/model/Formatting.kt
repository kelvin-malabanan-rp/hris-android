package io.rocketpartners.hris.model

import java.util.Locale

/**
 * Human-readable file size, e.g. "47 KB", "1.2 MB". Base-1000 units to match iOS
 * `ByteCountFormatter(.file)`. Used by [Payslip], [TicketAttachment], and [UploadFile].
 */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1000) return "$bytes bytes"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1000.0
    var unitIndex = 0
    while (value >= 1000.0 && unitIndex < units.size - 1) {
        value /= 1000.0
        unitIndex++
    }
    // Round to one decimal, then drop a trailing ".0" so "2.0 KB" reads "2 KB" but "1.2 MB" stays.
    val rounded = Math.round(value * 10.0) / 10.0
    val number = if (rounded % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", rounded)
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
    return "$number ${units[unitIndex]}"
}
