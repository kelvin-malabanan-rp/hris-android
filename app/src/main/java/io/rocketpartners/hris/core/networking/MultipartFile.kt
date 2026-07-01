package io.rocketpartners.hris.core.networking

/** One file part of a `multipart/form-data` upload. Mirrors iOS `MultipartFile`. */
class MultipartFile(
    val filename: String,
    val mimeType: String,
    val data: ByteArray,
)
