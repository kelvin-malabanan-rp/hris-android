package io.rocketpartners.hris.core.networking

/**
 * All networking failures, each carrying a user-facing [userMessage]. Mirrors iOS `APIError`.
 */
sealed class ApiError(message: String? = null) : Exception(message) {
    object InvalidUrl : ApiError()
    data class Network(val detail: String) : ApiError(detail)
    data class Decoding(val detail: String) : ApiError(detail)
    object Unauthorized : ApiError()
    data class Server(val serverMessage: String, val status: Int) : ApiError(serverMessage)
    object Unknown : ApiError()

    /** A message safe to surface directly in the UI. */
    val userMessage: String
        get() = when (this) {
            is InvalidUrl -> "Something went wrong building the request."
            is Network -> "A network error occurred. Please try again."
            is Decoding -> "We received an unexpected response from the server."
            is Unauthorized -> "Your session has expired. Please sign in again."
            is Server -> serverMessage
            is Unknown -> "An unexpected error occurred."
        }
}
