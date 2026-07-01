package io.rocketpartners.hris.core.networking

import kotlinx.serialization.Serializable

/**
 * A Spring `PagedResponse<T>` envelope. [content] is always present; the paging metadata is
 * optional so single-page call sites keep decoding unchanged. Terminate a load-more loop on
 * `last == true` (there is no `hasNext`). Mirrors iOS `Paged`.
 */
@Serializable
data class Paged<T>(
    val content: List<T>,
    val last: Boolean? = null,
    val totalElements: Int? = null,
)
