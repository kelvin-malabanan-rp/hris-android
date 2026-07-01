package io.rocketpartners.hris.app

import io.rocketpartners.hris.BuildConfig
import io.rocketpartners.hris.core.networking.mock.MockInterceptor
import okhttp3.OkHttpClient

/**
 * Single source of truth for the backend base URL + OkHttp client construction, including `-mock`
 * mode. Mirrors iOS `BackendConfig`. The base URL is a `buildConfigField` so it is resolved by the
 * build type.
 */
object BackendConfig {
    val baseUrl: String get() = BuildConfig.BASE_URL

    /**
     * Real backend client by default; installs the in-process [MockInterceptor] when [mock] is set.
     * [autoLogin] is forwarded to the interceptor to emulate a pre-authenticated session.
     */
    fun makeClient(mock: Boolean, autoLogin: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (mock) builder.addInterceptor(MockInterceptor(autoLogin))
        return builder.build()
    }
}
