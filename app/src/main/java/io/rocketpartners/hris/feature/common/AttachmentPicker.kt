package io.rocketpartners.hris.feature.common

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.UploadFile
import java.util.UUID

/** Max images per ticket message, matching the iOS PhotosPicker cap. */
const val MAX_ATTACHMENTS = 5

/**
 * Reads a picked image [uri] into an [UploadFile] (bytes + MIME + a unique JPEG/PNG filename so the
 * multipart upload doesn't collide on identical names). Returns null if the stream can't be read.
 * Mirrors the iOS `loadPhotos` conversion.
 */
fun readUploadFile(resolver: ContentResolver, uri: Uri): UploadFile? {
    val mime = resolver.getType(uri) ?: "image/jpeg"
    val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() ?: return null
    return UploadFile(filename = "photo-${UUID.randomUUID()}.${imageExtension(mime)}", mimeType = mime, data = bytes)
}

/** File extension for a picked image's MIME type (PNG stays PNG; everything else is treated as JPEG). */
fun imageExtension(mime: String?): String = if (mime?.contains("png") == true) "png" else "jpg"

/**
 * Remembers an Android Photo Picker launcher (no runtime permission needed) that reads the chosen
 * images and reports the ones within the 10 MB cap, plus whether any were rejected for size.
 * Returns a `launch` lambda. Mirrors the iOS `PhotosPicker` + size-limit flow.
 */
@Composable
fun rememberImageAttachmentPicker(onResult: (added: List<UploadFile>, anyTooLarge: Boolean) -> Unit): () -> Unit {
    val resolver = LocalContext.current.contentResolver
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_ATTACHMENTS)) { uris ->
        val files = uris.mapNotNull { readUploadFile(resolver, it) }
        val within = files.filter { it.isWithinSizeLimit }
        onResult(within, within.size != files.size)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Horizontal chips for pending attachments, each removable. Mirrors the iOS `attachmentChips`. */
@Composable
fun AttachmentChips(files: List<UploadFile>, onRemove: (UploadFile) -> Unit, modifier: Modifier = Modifier) {
    if (files.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        files.forEach { file ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(Theme.Radius.control))
                    .padding(start = Theme.Spacing.md, end = Theme.Spacing.xs, top = Theme.Spacing.xs, bottom = Theme.Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xs),
            ) {
                Text(file.formattedSize, style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = { onRemove(file) }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove attachment", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
