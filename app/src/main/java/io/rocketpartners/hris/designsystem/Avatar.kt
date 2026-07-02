package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.platform.LocalContext

/**
 * Circular avatar: remote image when available (via Coil), tinted initials otherwise. Decorative —
 * hidden from accessibility since the name is shown as adjacent text. Mirrors iOS `Avatar`.
 */
@Composable
fun Avatar(
    name: String?,
    modifier: Modifier = Modifier,
    model: Any? = null,
    size: Dp = 40.dp,
    accent: Theme.Accent = Theme.Accent.INFO,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        // A String model is a wire image reference (often a relative `/uploads` path); resolve it to
        // a full URL so the authenticated Coil loader can fetch it. Non-String models pass through.
        val resolved: Any? = if (model is String) io.rocketpartners.hris.core.networking.ImageUrls.resolve(model) else model
        if (resolved != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(resolved).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
                loading = { Initials(name, size, accent) },
                error = { Initials(name, size, accent) },
                success = { SubcomposeAsyncImageContent() },
            )
        } else {
            Initials(name, size, accent)
        }
    }
}

@Composable
private fun Initials(name: String?, size: Dp, accent: Theme.Accent) {
    Box(
        modifier = Modifier
            .size(size)
            .background(accent.tint.copy(alpha = Theme.Opacity.surface), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = Initials.from(name),
            color = accent.tint,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}
