package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/** Bearer + refresh token pair persisted after login. Mirrors iOS `AuthTokens`. */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)
