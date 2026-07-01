package io.rocketpartners.hris.core.auth

/**
 * Supplies bearer tokens to the [io.rocketpartners.hris.core.networking.ApiClient] and performs
 * refresh on demand. Mirrors iOS `TokenProviding`.
 */
interface TokenProviding {
    suspend fun accessToken(): String?

    /** Performs a token refresh; throws if refresh is not possible. */
    suspend fun refresh()
}
