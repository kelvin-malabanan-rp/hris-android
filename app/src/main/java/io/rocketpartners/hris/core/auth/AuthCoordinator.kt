package io.rocketpartners.hris.core.auth

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.core.networking.AppJson
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.model.AuthTokens
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Owns token refresh. Coalesces concurrent refresh requests into one network call — mirrors the iOS
 * `AuthCoordinator` actor, using a [Mutex] + a shared in-flight [Deferred] in place of Swift's
 * single `Task`. Refreshes hit `/auth/refresh` directly (bypassing [ApiError] auth) to avoid
 * recursion with the API client.
 */
class AuthCoordinator(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val store: TokenStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : TokenProviding {

    private val mutex = Mutex()
    private var inFlight: Deferred<Unit>? = null

    override suspend fun accessToken(): String? = store.tokens()?.accessToken

    override suspend fun refresh() {
        val job = mutex.withLock {
            inFlight ?: scope.async { performRefresh() }.also { inFlight = it }
        }
        try {
            job.await()
        } finally {
            mutex.withLock { if (inFlight === job) inFlight = null }
        }
    }

    private suspend fun performRefresh() {
        val refreshToken = store.tokens()?.refreshToken ?: throw ApiError.Unauthorized
        val bodyJson = AppJson.encodeToString(mapOf("refreshToken" to refreshToken))
        val endpoint = Endpoint(
            path = "auth/refresh",
            method = Endpoint.Method.POST,
            body = bodyJson.encodeToByteArray(),
            requiresAuth = false,
        )
        val (bytes, code) = execute(endpoint.buildRequest(baseUrl))
        if (code !in 200..299) {
            // 401/403 → refresh token is genuinely invalid; other statuses are server faults.
            if (code == 401 || code == 403) throw ApiError.Unauthorized
            throw ApiError.Server("Token refresh failed.", code)
        }
        val tokens = try {
            val data = AppJson.parseToJsonElement(bytes.decodeToString()).jsonObject["data"]
            if (data == null || data is JsonNull) throw ApiError.Unauthorized
            AppJson.decodeFromJsonElement(AuthTokens.serializer(), data)
        } catch (e: ApiError) {
            throw e
        } catch (_: Exception) {
            throw ApiError.Unauthorized
        }
        store.save(tokens)
    }

    private suspend fun execute(request: Request): Pair<ByteArray, Int> = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                (response.body?.bytes() ?: ByteArray(0)) to response.code
            }
        } catch (e: IOException) {
            throw ApiError.Network(e.message ?: "Network error")
        }
    }
}
