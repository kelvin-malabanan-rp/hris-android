package io.rocketpartners.hris.core.networking

import kotlinx.serialization.json.Json

/**
 * Shared kotlinx.serialization config. Mirrors the iOS `JSONCoders` decoder's leniency:
 * - [Json.ignoreUnknownKeys]: the backend sends fields we don't model (reactions, comments, …).
 * - [Json.coerceInputValues]: an explicit `null` for a field with a non-null default coerces to the
 *   default — matches Swift's `decodeIfPresent(...) ?? default`.
 * - `explicitNulls = false`: null fields are omitted on encode, matching the backend's
 *   `@JsonInclude(NON_NULL)` request-body expectation.
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
