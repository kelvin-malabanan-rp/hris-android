package io.rocketpartners.hris.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.app.AppEnvironment
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.LeaveDateText
import io.rocketpartners.hris.designsystem.ScreenHeader
import io.rocketpartners.hris.designsystem.StatusBadge
import io.rocketpartners.hris.designsystem.Theme

/** Global search across leave, WFH, events, and people on leave. Mirrors iOS `GlobalSearchView`. */
@Composable
fun SearchScreen(environment: AppEnvironment, modifier: Modifier = Modifier) {
    val store = remember { GlobalSearchStore(environment.leaveRepository, environment.wfhRepository, environment.calendarRepository) }
    val state by store.state.collectAsState()
    LaunchedEffect(Unit) { store.load() }

    Column(modifier.fillMaxSize().padding(horizontal = Theme.Spacing.lg)) {
        ScreenHeader("Search")
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!state.hasQuery || !state.hasResults) {
                EmptyState()
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
                    if (state.matchedLeaves.isNotEmpty()) Section("Leave Requests") {
                        state.matchedLeaves.forEach { l ->
                            ResultRow(l.leaveTypeName ?: "Leave", LeaveDateText.range(l.startDate, l.endDate)) { StatusBadge(text = l.displayStatus, rawStatus = l.status) }
                        }
                    }
                    if (state.matchedSchedules.isNotEmpty()) Section("Work From Home") {
                        state.matchedSchedules.forEach { s -> ResultRow(s.date, s.dayName ?: "WFH") { s.status?.let { StatusBadge(rawStatus = it) } } }
                    }
                    if (state.matchedEvents.isNotEmpty()) Section("Calendar") {
                        state.matchedEvents.forEach { e -> ResultRow(e.title, e.type?.replaceFirstChar(Char::uppercase) ?: "Event") {} }
                    }
                    if (state.matchedPeople.isNotEmpty()) Section("People on Leave") {
                        state.matchedPeople.forEach { p -> ResultRow(p.user.name, p.leaveType.name) {} }
                    }
                    Box(Modifier.size(Theme.Spacing.lg))
                }
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = store::setQuery,
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = Theme.Spacing.md),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Box(Modifier.size(64.dp).background(Theme.brand.copy(alpha = Theme.Opacity.fill), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(28.dp))
            }
            Text("Search", style = MaterialTheme.typography.titleMedium)
            Text("Find leave requests, WFH days, events, and people.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    ContentCard(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}
