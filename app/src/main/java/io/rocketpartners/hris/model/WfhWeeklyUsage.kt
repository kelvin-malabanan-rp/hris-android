package io.rocketpartners.hris.model

import kotlinx.serialization.Serializable

/** Current-week WFH quota usage from `/wfh/weekly-usage`. Mirrors iOS `WfhWeeklyUsage`. */
@Serializable
data class WfhWeeklyUsage(
    val used: Int,
    val quota: Int,
    val remaining: Int,
    /** Applications this week awaiting manager approval. Older payloads omit it → 0. */
    val pending: Int = 0,
)
