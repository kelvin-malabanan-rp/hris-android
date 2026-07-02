package io.rocketpartners.hris.feature.notifications

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.model.AppNotification
import io.rocketpartners.hris.designsystem.EmptyState
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.FilterChip
import io.rocketpartners.hris.designsystem.FilterChipStyle
import io.rocketpartners.hris.designsystem.Theme
import kotlinx.coroutines.launch

/**
 * Notification inbox: a filter-chip bar (All / Unread / Approvals / Tickets) above a list sectioned
 * by time (Today / Yesterday / Last 7 days / Earlier). Rows open a detail screen that marks the
 * notification read and can route to its destination. Mirrors iOS `NotificationInboxView`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationInboxScreen(
    repository: NotificationRepository,
    onBack: () -> Unit,
    onRoute: (referenceType: String?, referenceId: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = remember { NotificationStore(repository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(InboxFilter.ALL) }
    var selected by remember { mutableStateOf<io.rocketpartners.hris.model.AppNotification?>(null) }

    LaunchedEffect(Unit) { store.load() }

    val detail = selected
    if (detail != null) {
        LaunchedEffect(detail.id) { store.markRead(detail) }
        NotificationDetailScreen(
            notification = detail,
            onBack = { selected = null },
            onRoute = { onRoute(detail.referenceType, detail.referenceId) },
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (state.unreadCount > 0) TextButton(onClick = { scope.launch { store.markAllRead() } }) { Text("Mark all read") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            InboxFilterBar(filter) { filter = it }
            Box(Modifier.fillMaxSize()) {
                val phase = state.phase
                when {
                    phase is Phase.Failed -> ErrorState(message = phase.message, retry = { store.load() })
                    (phase is Phase.Idle || phase is Phase.Loading) && state.notifications.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else -> InboxList(
                        groups = InboxGrouping.groups(state.notifications.filter { filter.matches(it) }),
                        filter = filter,
                        refreshing = phase is Phase.Loading,
                        onRefresh = { scope.launch { store.load() } },
                        onOpen = { selected = it },
                        onMarkRead = { scope.launch { store.markRead(it) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun InboxList(
    groups: List<InboxGroup>,
    filter: InboxFilter,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (AppNotification) -> Unit,
    onMarkRead: (AppNotification) -> Unit,
) {
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        if (groups.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.NotificationsNone,
                title = "No notifications",
                message = filter.emptyMessage,
                modifier = Modifier.fillMaxSize().padding(Theme.Spacing.xl),
            )
            return@PullToRefreshBox
        }
        LazyColumn(Modifier.fillMaxSize()) {
            groups.forEach { group ->
                stickyHeader(key = group.title) { SectionHeader(group.title) }
                items(group.items, key = { it.id }) { notification ->
                    SwipeableInboxRow(notification, onOpen = { onOpen(notification) }, onMarkRead = { onMarkRead(notification) })
                }
            }
        }
    }
}

/** Wraps a row in a swipe-to-mark-read gesture (only for unread rows); the row snaps back after the
 *  swipe fires. Mirrors iOS's trailing "Read" swipe action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableInboxRow(notification: AppNotification, onOpen: () -> Unit, onMarkRead: () -> Unit) {
    if (notification.isRead) {
        InboxRow(notification, Modifier.fillMaxWidth().clickable(onClick = onOpen))
        return
    }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // Fire mark-read on any completed swipe, but never actually dismiss — return false to snap back.
            if (value != SwipeToDismissBoxValue.Settled) onMarkRead()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = { ReadSwipeBackground(state.dismissDirection) },
        content = {
            InboxRow(
                notification,
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).clickable(onClick = onOpen),
            )
        },
    )
}

/** The brand-colored "Read" affordance revealed behind a swiping row, edge-aligned to the drag. */
@Composable
private fun ReadSwipeBackground(direction: SwipeToDismissBoxValue) {
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    Box(Modifier.fillMaxSize().background(Theme.brand).padding(horizontal = Theme.Spacing.lg), contentAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.xs)) {
            Icon(Icons.Filled.MarkEmailRead, contentDescription = null, tint = Color.White)
            Text("Read", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.sm),
    )
}

/** Horizontal chip row selecting an [InboxFilter]; mirrors iOS `InboxFilterBar` (solid selected). */
@Composable
private fun InboxFilterBar(selection: InboxFilter, onSelect: (InboxFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
    ) {
        InboxFilter.entries.forEach { candidate ->
            FilterChip(candidate.label, candidate == selection, { onSelect(candidate) }, style = FilterChipStyle.Solid)
        }
    }
}
