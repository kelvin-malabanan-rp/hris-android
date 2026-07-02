package io.rocketpartners.hris.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import io.rocketpartners.hris.core.auth.TokenProviding
import io.rocketpartners.hris.core.networking.AuthedImageLoader

/**
 * Process-wide Application. Also the global Coil [ImageLoaderFactory]: every `AsyncImage` in the app
 * loads through an [AuthedImageLoader] that attaches the bearer token for backend `/uploads` images
 * (mirrors iOS `AuthedAsyncImage`). The token provider is supplied by [AppEnvironment] once it's
 * built in `MainActivity`; the interceptor reads it lazily, so images before login simply load
 * without a token.
 */
class HrisApp : Application(), ImageLoaderFactory {

    /** Set by [MainActivity] after the composition root is created. Read per image request. */
    @Volatile
    var tokenProvider: TokenProviding? = null

    override fun newImageLoader(): ImageLoader =
        AuthedImageLoader.create(this) { tokenProvider }
}
