package io.rocketpartners.hris.core.networking.mock

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor that returns canned JSON (in the real `ApiResponse` envelope) or binary bytes,
 * so the whole app runs fully populated with no backend. Installed only in mock mode. Mirrors iOS
 * `MockURLProtocol`. Routing is pure (keyed off path + method), so there is no shared mutable state.
 *
 * [autoLogin] emulates a token-gated session: launch bootstrap (`GET /auth/me` with no stored token)
 * 401s → the app shows Login. When true, `/auth/me` resolves without a token.
 */
class MockInterceptor(private val autoLogin: Boolean = false) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val hasAuth = request.header("Authorization") != null

        // Binary endpoints return raw bytes, NOT the JSON envelope.
        if (method == "GET" && (path.endsWith("/download") || path.contains("/files/"))) {
            return binary(chain, "application/pdf", "%PDF-1.4\nMock document\n%%EOF\n".toByteArray())
        }
        if (method == "GET" && path.contains("/uploads/images/")) {
            val png = Base64.decode(ONE_BY_ONE_PNG, Base64.DEFAULT)
            return binary(chain, "image/png", png)
        }

        val (status, body) =
            if (method == "GET" && path.endsWith("/auth/me") && !hasAuth && !autoLogin) {
                401 to """{"success":false,"message":"Unauthorized","data":null}"""
            } else {
                200 to """{"success":true,"message":null,"data":${MockFixtures.dataPayload(method, path)}}"""
            }
        return json(chain, status, body)
    }

    private fun json(chain: Interceptor.Chain, status: Int, body: String): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message(if (status in 200..299) "OK" else "Error")
            .body(body.toResponseBody(JSON))
            .build()

    private fun binary(chain: Interceptor.Chain, contentType: String, bytes: ByteArray): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(bytes.toResponseBody(contentType.toMediaType()))
            .build()

    private companion object {
        val JSON = "application/json".toMediaType()
        const val ONE_BY_ONE_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    }
}
