package io.rocketpartners.hris.core.networking

import io.rocketpartners.hris.BuildConfig

/**
 * Resolves a wire image reference to a full, loadable URL. The backend serves `/uploads` behind
 * auth relative to the API base, so a bare path like `/uploads/images/x.jpg` must be joined onto the
 * base URL before Coil can fetch it (and [AuthedImageLoader] then attaches the bearer token for that
 * host). Absolute `http(s)` URLs are returned unchanged. Mirrors the iOS `AuthedAsyncImage` contract
 * where the repository joins the relative path onto `APIClient`'s base.
 */
object ImageUrls {
    /** Full URL for [path], or `null` when there's nothing to load. */
    fun resolve(path: String?): String? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val suffix = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return base + suffix
    }
}
