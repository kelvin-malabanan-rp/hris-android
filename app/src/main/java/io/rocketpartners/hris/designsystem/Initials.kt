package io.rocketpartners.hris.designsystem

/** Derives up-to-two-letter initials from a display name. Returns "?" when empty. Mirrors iOS. */
object Initials {
    fun from(name: String?): String {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isEmpty()) return "?"
        val letters = trimmed.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
        return if (letters.isEmpty()) "?" else letters.joinToString("").uppercase()
    }
}
