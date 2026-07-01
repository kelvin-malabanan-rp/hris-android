package io.rocketpartners.hris.core.networking

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * A value type describing one HTTP call: path (relative to the API base), method, query, optional
 * JSON body, and whether it needs bearer auth. [buildRequest] assembles the OkHttp [Request].
 * Mirrors iOS `Endpoint`.
 */
data class Endpoint(
    val path: String,
    val method: Method = Method.GET,
    val query: List<Pair<String, String>> = emptyList(),
    val body: ByteArray? = null,
    val requiresAuth: Boolean = true,
) {
    enum class Method { GET, POST, PUT, PATCH, DELETE }

    /** Builds the request against [baseUrl]; throws [ApiError.InvalidUrl] if the URL is malformed. */
    fun buildRequest(baseUrl: String): Request {
        val base = baseUrl.toHttpUrlOrNull() ?: throw ApiError.InvalidUrl
        val urlBuilder: HttpUrl.Builder = base.newBuilder()
        // Append each path segment so a multi-segment `path` (e.g. "leave-applications/1") lands
        // under the base's existing path rather than replacing it.
        path.trim('/').split('/').filter { it.isNotEmpty() }.forEach { urlBuilder.addPathSegment(it) }
        query.forEach { (name, value) -> urlBuilder.addQueryParameter(name, value) }
        val url = urlBuilder.build()

        val requestBody: RequestBody? = body?.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        when (method) {
            Method.GET -> builder.get()
            Method.DELETE -> if (requestBody != null) builder.delete(requestBody) else builder.delete()
            Method.POST -> builder.post(requestBody ?: EMPTY_BODY)
            Method.PUT -> builder.put(requestBody ?: EMPTY_BODY)
            Method.PATCH -> builder.patch(requestBody ?: EMPTY_BODY)
        }
        if (requestBody != null) builder.header("Content-Type", "application/json")
        return builder.build()
    }

    // Data classes with an array member need explicit equals/hashCode to compare by contents.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Endpoint) return false
        return path == other.path &&
            method == other.method &&
            query == other.query &&
            requiresAuth == other.requiresAuth &&
            (body?.contentEquals(other.body) ?: (other.body == null))
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + query.hashCode()
        result = 31 * result + requiresAuth.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
    }
}
