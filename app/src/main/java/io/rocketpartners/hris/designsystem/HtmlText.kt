package io.rocketpartners.hris.designsystem

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

/**
 * Renders backend HTML (announcement bodies) safely. Uses [HtmlCompat.fromHtml] — a static parser
 * that does NOT execute scripts (no stored-XSS), unlike a WebView. Mirrors iOS `HTMLText`;
 * [plainText] is the deterministic safe baseline exercised by the unit tests.
 */
@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    val contentColor = LocalContentColor.current.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = { view ->
            view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            view.setTextColor(contentColor)
        },
    )
}

/**
 * Strips `<script>`/`<style>` blocks entirely, removes remaining tags, decodes common entities, and
 * collapses whitespace. Safe and deterministic — never executes anything. Mirrors iOS
 * `HTMLText.plainText`.
 */
fun htmlToPlainText(html: String): String {
    var s = html
    for (tag in listOf("script", "style")) {
        s = s.replace(Regex("<$tag[^>]*>[\\s\\S]*?</$tag>", RegexOption.IGNORE_CASE), " ")
    }
    s = s.replace(Regex("<[^>]+>"), " ")
    val entities = mapOf(
        "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
        "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'",
    )
    for ((entity, value) in entities) {
        s = s.replace(entity, value)
    }
    s = s.replace(Regex("\\s+"), " ")
    return s.trim()
}
