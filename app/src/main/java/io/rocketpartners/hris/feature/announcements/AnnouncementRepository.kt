package io.rocketpartners.hris.feature.announcements

import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.Endpoint
import io.rocketpartners.hris.core.networking.Paged
import io.rocketpartners.hris.core.networking.send
import io.rocketpartners.hris.model.Announcement
import io.rocketpartners.hris.model.pinnedFirst

interface AnnouncementRepository {
    /** One page of the company feed (server orders pinned-first, then `publishedAt DESC`). */
    suspend fun feed(page: Int, category: String?, search: String?): List<Announcement>

    /** A single announcement with its HTML body + images. */
    suspend fun detail(id: Int): Announcement

    /** Raw bytes for an auth-required `/uploads` image path. Binary (no envelope). */
    suspend fun imageData(path: String): ByteArray
}

class LiveAnnouncementRepository(private val client: ApiClient) : AnnouncementRepository {

    override suspend fun feed(page: Int, category: String?, search: String?): List<Announcement> {
        val query = buildList {
            add("page" to "$page")
            add("size" to "$PAGE_SIZE")
            category?.takeIf { it.isNotEmpty() }?.let { add("category" to it) }
            search?.takeIf { it.isNotEmpty() }?.let { add("search" to it) }
        }
        val result: Paged<Announcement> =
            client.send(Endpoint("announcements", query = query), Paged.serializer(Announcement.serializer()))
        return result.content.pinnedFirst()
    }

    override suspend fun detail(id: Int): Announcement = client.send(Endpoint("announcements/$id"))

    override suspend fun imageData(path: String): ByteArray =
        client.sendData(Endpoint(normalize(path)))

    private companion object {
        const val PAGE_SIZE = 10

        /** Strips scheme+host and any `/api/v1` prefix so a wire URL becomes an appendable path. */
        fun normalize(path: String): String {
            var p = path
            Regex("^https?://[^/]+").find(p)?.let { p = p.substring(it.range.last + 1) }
            val apiIndex = p.indexOf("/api/v1")
            if (apiIndex >= 0) p = p.substring(apiIndex + "/api/v1".length)
            return if (p.startsWith("/")) p else "/$p"
        }
    }
}
