package io.rocketpartners.hris.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.designsystem.ScreenHeader
import io.rocketpartners.hris.designsystem.Theme

/**
 * Global search. The query field + results are wired in Phase 5; for now it shows the iOS empty
 * state (a tinted search glyph, title, and hint) with the search field pinned at the bottom.
 */
@Composable
fun SearchScreen(modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(horizontal = Theme.Spacing.lg)) {
        ScreenHeader("Search")
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                Box(
                    Modifier.size(64.dp).background(Theme.brand.copy(alpha = Theme.Opacity.fill), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(28.dp))
                }
                Text("Search", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Find leave requests, WFH days, events, and people.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = Theme.Spacing.md),
        )
    }
}
