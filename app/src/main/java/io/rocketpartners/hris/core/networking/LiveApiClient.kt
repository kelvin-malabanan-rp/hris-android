package io.rocketpartners.hris.core.networking

import io.rocketpartners.hris.core.auth.TokenProviding
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OkHttp-backed [ApiClient]. Applies bearer auth from the [TokenProviding] and does a single
 * transparent refresh+retry on a 401, then unwraps the `ApiResponse` envelope. Mirrors iOS
 * `LiveAPIClient`.
 */
class LiveApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val tokenProvider: TokenProviding?,
) : ApiClient {

    private class Raw(val bytes: ByteArray, val code: Int)

    override suspend fun <T> send(endpoint: Endpoint, serializer: KSerializer<T>): T {
        val raw = perform(endpoint, allowRetry = true)
        throwIfError(raw)
        return decodeEnvelope(raw.bytes, serializer)
    }

    override suspend fun sendVoid(endpoint: Endpoint) {
        throwIfError(perform(endpoint, allowRetry = true))
    }

    override suspend fun sendData(endpoint: Endpoint): ByteArray {
        val raw = perform(endpoint, allowRetry = true)
        throwIfError(raw)
        return raw.bytes
    }

    override suspend fun <T> sendMultipart(
        endpoint: Endpoint,
        jsonPartName: String,
        jsonData: ByteArray,
        filePartName: String,
        files: List<MultipartFile>,
        serializer: KSerializer<T>,
    ): T {
        val raw = performMultipart(endpoint, jsonPartName, jsonData, filePartName, files, allowRetry = true)
        throwIfError(raw)
        return decodeEnvelope(raw.bytes, serializer)
    }

    // --- request execution --------------------------------------------------

    /** Executes [endpoint] with bearer auth and a single refresh+retry on 401. */
    private suspend fun perform(endpoint: Endpoint, allowRetry: Boolean): Raw {
        val token = if (endpoint.requiresAuth) tokenProvider?.accessToken() else null
        var request = endpoint.buildRequest(baseUrl)
        if (token != null) {
            request = request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        val raw = execute(request)
        if (raw.code == 401 && endpoint.requiresAuth && allowRetry && tokenProvider != null) {
            try {
                tokenProvider.refresh()
            } catch (_: Exception) {
                return raw // refresh failed; caller maps 401 to unauthorized
            }
            return perform(endpoint, allowRetry = false)
        }
        return raw
    }

    private suspend fun performMultipart(
        endpoint: Endpoint,
        jsonPartName: String,
        jsonData: ByteArray,
        filePartName: String,
        files: List<MultipartFile>,
        allowRetry: Boolean,
    ): Raw {
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(jsonPartName, null, jsonData.toRequestBody(JSON_MEDIA_TYPE))
        for (file in files) {
            multipart.addFormDataPart(filePartName, file.filename, file.data.toRequestBody(file.mimeType.toMediaType()))
        }
        val url = endpoint.buildRequest(baseUrl).url
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .method(endpoint.method.name, multipart.build())
        if (endpoint.requiresAuth) {
            tokenProvider?.accessToken()?.let { builder.header("Authorization", "Bearer $it") }
        }
        val raw = execute(builder.build())
        if (raw.code == 401 && endpoint.requiresAuth && allowRetry && tokenProvider != null) {
            try {
                tokenProvider.refresh()
            } catch (_: Exception) {
                return raw
            }
            return performMultipart(endpoint, jsonPartName, jsonData, filePartName, files, allowRetry = false)
        }
        return raw
    }

    private suspend fun execute(request: Request): Raw = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                Raw(response.body?.bytes() ?: ByteArray(0), response.code)
            }
        } catch (e: IOException) {
            throw ApiError.Network(e.message ?: "Network error")
        }
    }

    // --- response handling --------------------------------------------------

    /** Maps a 401 to [ApiError.Unauthorized] and any other non-2xx to [ApiError.Server]. */
    private fun throwIfError(raw: Raw) {
        if (raw.code == 401) throw ApiError.Unauthorized
        if (raw.code !in 200..299) {
            throw ApiError.Server(decodeMessage(raw.bytes) ?: "Request failed.", raw.code)
        }
    }

    private fun <T> decodeEnvelope(bytes: ByteArray, serializer: KSerializer<T>): T {
        val data = try {
            AppJson.parseToJsonElement(bytes.decodeToString()).jsonObject["data"]
        } catch (e: Exception) {
            throw ApiError.Decoding(e.message ?: "Invalid JSON")
        }
        if (data == null || data is JsonNull) throw ApiError.Decoding("Missing data in response")
        return try {
            AppJson.decodeFromJsonElement(serializer, data)
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError.Decoding(e.message ?: "Decode failed")
        }
    }

    private fun decodeMessage(bytes: ByteArray): String? = try {
        AppJson.parseToJsonElement(bytes.decodeToString()).jsonObject["message"]?.jsonPrimitive?.contentOrNull
    } catch (e: Exception) {
        null
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
