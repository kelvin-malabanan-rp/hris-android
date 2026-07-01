package io.rocketpartners.hris.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Full-screen failed state: icon + message + a Retry button wired to a suspend reload. The button
 * shows an in-place spinner while retrying. Mirrors iOS `ErrorStateView`.
 */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Couldn't load",
    icon: ImageVector = Icons.Outlined.Warning,
    retry: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Theme.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            enabled = !isRetrying,
            onClick = {
                scope.launch {
                    isRetrying = true
                    retry()
                    isRetrying = false
                }
            },
        ) {
            if (isRetrying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Try Again", modifier = Modifier.padding(start = Theme.Spacing.sm))
            }
        }
    }
}

/**
 * Compact inline failed state for a single card/section (not a whole screen). Mirrors iOS
 * `InlineErrorView`.
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier,
    retry: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Theme.Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            enabled = !isRetrying,
            onClick = {
                scope.launch {
                    isRetrying = true
                    retry()
                    isRetrying = false
                }
            },
        ) {
            if (isRetrying) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Retry", modifier = Modifier.padding(start = Theme.Spacing.xs))
            }
        }
    }
}
