package io.rocketpartners.hris.core.networking

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Sends [Endpoint]s and unwraps the `ApiResponse` envelope. Bearer auth + a single transparent
 * 401 refresh+retry live in the implementation. Mirrors iOS `APIClient`.
 *
 * The core methods take an explicit [KSerializer] (interfaces can't be `reified`); prefer the
 * reified `send`/`sendMultipart` extensions below at call sites.
 */
interface ApiClient {
    suspend fun <T> send(endpoint: Endpoint, serializer: KSerializer<T>): T

    /** For endpoints whose successful response carries `data: null`. */
    suspend fun sendVoid(endpoint: Endpoint)

    /** Raw bytes for a binary body (e.g. a PDF), bypassing the envelope decode. */
    suspend fun sendData(endpoint: Endpoint): ByteArray

    /** `multipart/form-data` upload: one named JSON part + 0..n repeatable file parts. */
    suspend fun <T> sendMultipart(
        endpoint: Endpoint,
        jsonPartName: String,
        jsonData: ByteArray,
        filePartName: String,
        files: List<MultipartFile>,
        serializer: KSerializer<T>,
    ): T
}

/** Reified convenience mirroring iOS `send<T>` — resolves the serializer from the call-site type. */
suspend inline fun <reified T> ApiClient.send(endpoint: Endpoint): T =
    send(endpoint, serializer())

/** Reified convenience mirroring iOS `sendMultipart<T>`. */
suspend inline fun <reified T> ApiClient.sendMultipart(
    endpoint: Endpoint,
    jsonPartName: String,
    jsonData: ByteArray,
    filePartName: String,
    files: List<MultipartFile>,
): T = sendMultipart(endpoint, jsonPartName, jsonData, filePartName, files, serializer())
