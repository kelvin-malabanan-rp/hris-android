package io.rocketpartners.hris.core.ui

import io.rocketpartners.hris.core.networking.ApiError

/** The load state machine every store exposes. Mirrors the iOS per-store `Phase` enum. */
sealed interface Phase {
    data object Idle : Phase
    data object Loading : Phase
    data object Loaded : Phase
    data class Failed(val message: String) : Phase
}

/** A user-facing message for a thrown error, preferring [ApiError.userMessage]. */
fun errorMessage(error: Throwable, fallback: String = "Couldn't load."): String =
    (error as? ApiError)?.userMessage ?: fallback
