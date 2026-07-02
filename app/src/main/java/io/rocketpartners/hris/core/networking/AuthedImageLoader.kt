package io.rocketpartners.hris.core.networking

import android.content.Context
import coil.ImageLoader
import io.rocketpartners.hris.BuildConfig
import io.rocketpartners.hris.core.auth.TokenProviding
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * Builds a Coil [ImageLoader] whose OkHttp client attaches `Authorization: Bearer <token>` to
 * requests bound for the backend host — the equivalent of iOS `AuthedAsyncImage` loading `/uploads`
 * bytes through the authenticated `APIClient`. Requests to any other host (e.g. a public CDN avatar)
 * are left untouched so we never leak the token off-host. The token is read lazily per request from
 * [tokenProvider], so the loader can be built before a session exists.
 */
object AuthedImageLoader {
    fun create(context: Context, tokenProvider: () -> TokenProviding?): ImageLoader {
        val backendHost = BuildConfig.BASE_URL.toHttpUrlOrNull()?.host
        val http = OkHttpClient.Builder()
            .addInterceptor(BearerImageInterceptor(backendHost, tokenProvider))
            .build()
        return ImageLoader.Builder(context)
            .okHttpClient(http)
            .crossfade(true)
            .build()
    }
}

/** Adds the bearer header only for [backendHost] requests; a no-op elsewhere. */
private class BearerImageInterceptor(
    private val backendHost: String?,
    private val tokenProvider: () -> TokenProviding?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (backendHost == null || request.url.host != backendHost) return chain.proceed(request)
        val token = runBlocking { tokenProvider()?.accessToken() } ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}
