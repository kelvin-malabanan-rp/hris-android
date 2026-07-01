package io.rocketpartners.hris.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Parses `#RRGGBB` (or `#RGB`); returns null if unparseable or null input. Mirrors iOS
 * `Color(hex:)`.
 */
fun hexColor(hex: String?): Color? {
    var value = hex ?: return null
    if (value.startsWith("#")) value = value.substring(1)
    if (value.length == 3) value = value.map { "$it$it" }.joinToString("")
    if (value.length != 6) return null
    val parsed = value.toLongOrNull(16) ?: return null
    val r = (parsed shr 16) and 0xFF
    val g = (parsed shr 8) and 0xFF
    val b = parsed and 0xFF
    return Color(r.toInt(), g.toInt(), b.toInt())
}
