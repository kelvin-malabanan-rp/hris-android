package io.rocketpartners.hris.feature.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.AssetAssignment
import java.time.Instant

/** The signed-in user's checked-out assets, with return-state chips. Mirrors iOS `MyAssetsView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAssetsScreen(repository: AssetRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = remember { MyAssetsStore(repository) }
    val state by store.state.collectAsState()
    val now = remember { Instant.now() }

    LaunchedEffect(Unit) { store.load() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Assets") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val phase = state.phase) {
                is Phase.Failed -> ErrorState(message = phase.message, retry = { store.load() })
                is Phase.Loaded -> if (state.assets.isEmpty()) {
                    EmptyState(icon = Icons.Filled.Inventory2, title = "No assets assigned", message = "Equipment checked out to you will appear here.", modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl))
                } else {
                    Column(Modifier.fillMaxSize().padding(Theme.Spacing.lg), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        state.assets.forEach { AssetCard(it, now) }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun AssetCard(asset: AssetAssignment, now: Instant) {
    ContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = Theme.brand)
                Column(Modifier.weight(1f)) {
                    Text(asset.assetName ?: "Asset", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val meta = listOfNotNull(asset.assetTag, asset.categoryName).joinToString(" · ")
                    if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ReturnChip(asset.returnState(now))
            }
            asset.expectedReturnDate?.let {
                Text("Return by $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            asset.checkoutNotes?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReturnChip(state: AssetAssignment.ReturnState) {
    val (label, color) = when (state) {
        AssetAssignment.ReturnState.OVERDUE -> "Overdue" to Theme.Accent.DANGER.tint
        AssetAssignment.ReturnState.DUE_SOON -> "Due soon" to Theme.Accent.PENDING.tint
        AssetAssignment.ReturnState.NONE -> return
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.background(color.copy(alpha = Theme.Opacity.fill), CircleShape).padding(horizontal = Theme.Spacing.sm, vertical = 2.dp),
    )
}
