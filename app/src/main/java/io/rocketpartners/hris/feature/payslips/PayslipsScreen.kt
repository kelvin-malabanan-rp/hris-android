package io.rocketpartners.hris.feature.payslips

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.Payslip
import java.io.File
import kotlinx.coroutines.launch

/** Payslips list with a pay-period filter; tapping a row downloads + opens the PDF. Mirrors iOS. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipsScreen(repository: PayslipRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = remember { PayslipsStore(repository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { store.load() }
    LaunchedEffect(state.downloadError) {
        state.downloadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            store.clearDownloadError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Payslips") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val phase = state.phase) {
                is Phase.Failed -> ErrorState(message = phase.message, retry = { store.load() })
                is Phase.Loaded -> if (state.payslips.isEmpty()) {
                    EmptyState(icon = Icons.Filled.Description, title = "No payslips yet", modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl))
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                        if (state.periods.isNotEmpty()) {
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
                                FilterChip(selected = state.selectedPeriod == null, onClick = { store.selectPeriod(null) }, label = { Text("All") })
                                state.periods.forEach { period ->
                                    FilterChip(selected = state.selectedPeriod == period, onClick = { store.selectPeriod(period) }, label = { Text(period) })
                                }
                            }
                        }
                        state.filtered.forEach { payslip ->
                            PayslipRow(payslip, downloading = payslip.id in state.downloadingIds) {
                                scope.launch {
                                    val bytes = store.download(payslip) ?: return@launch
                                    openPdf(context, store.safeFileName(payslip), bytes)
                                }
                            }
                        }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun PayslipRow(payslip: Payslip, downloading: Boolean, onOpen: () -> Unit) {
    ContentCard(Modifier.fillMaxWidth().let { it }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(Theme.Size.iconInline))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
                Text(payslip.payPeriodLabel ?: "Payslip", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                val meta = listOfNotNull(payslip.cutoffDate, payslip.formattedSize).joinToString(" · ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (downloading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onOpen) { Icon(Icons.Filled.Download, contentDescription = "Open payslip", tint = Theme.brand) }
            }
        }
    }
}

/** Writes [bytes] to the shared cache dir and launches a PDF viewer via [FileProvider]. */
private fun openPdf(context: android.content.Context, fileName: String, bytes: ByteArray) {
    val dir = File(context.cacheDir, "payslips").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No PDF viewer installed.", Toast.LENGTH_SHORT).show()
    }
}
