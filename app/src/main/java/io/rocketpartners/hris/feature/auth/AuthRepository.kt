package io.rocketpartners.hris.feature.auth

import io.rocketpartners.hris.core.auth.TokenStore
import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.AuthTokens
import io.rocketpartners.hris.model.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Auth operations backing [AuthService]. Mirrors iOS `AuthRepository`. */
interface AuthRepository {
    suspend fun login(email: String, password: String): User
    suspend fun currentUser(): User
    suspend fun logout()
}

@Serializable
private data class LoginRequestBody(val email: String, val password: String)

/** `/auth/login` returns only tokens; the user is fetched separately from `/auth/me`. */
@Serializable
private data class LoginTokens(val accessToken: String, val refreshToken: String)

class LiveAuthRepository(
    private val client: ApiClient,
    private val store: TokenStore,
) : AuthRepository {

    override suspend fun login(email: String, password: String): User {
        val body = AppJson.encodeToString(LoginRequestBody(email, password)).encodeToByteArray()
        val endpoint = Endpoint("auth/login", Endpoint.Method.POST, body = body, requiresAuth = false)
        val tokens: LoginTokens = client.send(endpoint)
        store.save(AuthTokens(tokens.accessToken, tokens.refreshToken))
        // Tokens are persisted; fetch the authenticated profile.
        return currentUser()
    }

    override suspend fun currentUser(): User = client.send(Endpoint("auth/me"))

    override suspend fun logout() {
        try {
            client.sendVoid(Endpoint("auth/logout", Endpoint.Method.POST))
        } catch (e: Exception) {
            store.clear()
            throw e
        }
        store.clear()
    }
}
