package io.rocketpartners.hris.model

import io.rocketpartners.hris.core.networking.WireDate
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A company announcement from `GET /announcements` / `/announcements/{id}`. Backend uses
 * `@JsonInclude(NON_NULL)` → most fields optional. [body] is **raw HTML** — render it with an
 * HTML-aware text component. Images are relative, auth-required `/uploads` paths. Mirrors iOS.
 */
@Serializable
data class Announcement(
    val id: Int,
    val title: String? = null,
    /** Raw HTML. */
    val body: String? = null,
    /** `COMPANY_NEWS | EVENTS | FUN | HR_UPDATES | GENERAL`. */
    val category: String? = null,
    /** Lombok serializes `isPinned()` → JSON key `pinned` (not `isPinned`). Absent ⇒ false. */
    val pinned: Boolean = false,
    val authorId: Int? = null,
    val authorName: String? = null,
    val authorPosition: String? = null,
    /** Relative, auth-required image path. */
    val authorImageUrl: String? = null,
    /** `LocalDateTime` ISO without offset (server-local). */
    val publishedAt: String? = null,
    val images: List<AnnouncementImage> = emptyList(),
    val commentsCount: Int? = null,
    val createdAt: String? = null,
) {
    /** Published timestamp parsed for display; falls back to [createdAt]. */
    val parsedPublished: Instant?
        get() = publishedAt?.let(WireDate::parse) ?: createdAt?.let(WireDate::parse)

    /** The category mapped to a human label, e.g. `COMPANY_NEWS` → "Company News". */
    val categoryLabel: String?
        get() {
            val c = category
            if (c.isNullOrEmpty()) return null
            return c.split('_')
                .filter { it.isNotEmpty() }
                .joinToString(" ") { it.substring(0, 1).uppercase() + it.substring(1).lowercase() }
        }

    /** Image paths in `sortOrder` order (null sort orders sink to the end, stably). */
    val sortedImagePaths: List<String>
        get() = images
            .withIndex()
            .sortedWith(compareBy({ it.value.sortOrder ?: Int.MAX_VALUE }, { it.index }))
            .mapNotNull { it.value.url }
}

/**
 * One image attached to an announcement (`{id,url,fileName,sortOrder}`). [url] is a relative,
 * auth-required path; display [fileName] (the path filename is an opaque UUID).
 */
@Serializable
data class AnnouncementImage(
    val id: Int,
    val url: String? = null,
    val fileName: String? = null,
    val sortOrder: Int? = null,
)

/**
 * Defensive stable pinned-first ordering. The server already returns pinned-first then
 * `publishedAt DESC`; this preserves that order while guaranteeing pinned items lead. Stable: items
 * with equal pin state keep their original order.
 */
fun List<Announcement>.pinnedFirst(): List<Announcement> =
    withIndex()
        .sortedWith(compareByDescending<IndexedValue<Announcement>> { it.value.pinned }.thenBy { it.index })
        .map { it.value }
