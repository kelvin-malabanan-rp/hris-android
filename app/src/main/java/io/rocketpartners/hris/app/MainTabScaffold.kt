package io.rocketpartners.hris.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.notifications.NotificationStore
import kotlinx.coroutines.delay
import io.rocketpartners.hris.feature.announcements.AnnouncementDetailScreen
import io.rocketpartners.hris.feature.announcements.AnnouncementsScreen
import io.rocketpartners.hris.feature.assets.MyAssetsScreen
import io.rocketpartners.hris.feature.calendar.CalendarScreen
import io.rocketpartners.hris.feature.home.HomeScreen
import io.rocketpartners.hris.feature.notifications.NotificationInboxScreen
import io.rocketpartners.hris.feature.payslips.PayslipsScreen
import io.rocketpartners.hris.feature.profile.ProfileScreen
import io.rocketpartners.hris.feature.search.SearchScreen
import io.rocketpartners.hris.feature.tickets.TicketDetailScreen
import io.rocketpartners.hris.feature.tickets.TicketsScreen
import io.rocketpartners.hris.feature.timeoff.ApprovalKind
import io.rocketpartners.hris.feature.timeoff.ApprovalsScreen
import io.rocketpartners.hris.feature.timeoff.ScheduleScreen
import io.rocketpartners.hris.model.Announcement
import io.rocketpartners.hris.model.User

/** The five destinations. The first four live in the tab-bar pill; [SEARCH] is a separate circle. */
enum class HrisTab(val key: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.GridView),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    TIME_OFF("leave", "Schedule", Icons.Filled.EditCalendar),
    ME("me", "Me", Icons.Filled.AccountCircle),
    SEARCH("search", "Search", Icons.Filled.Search);

    companion object {
        val MAIN = listOf(HOME, CALENDAR, TIME_OFF, ME)
        fun fromKey(key: String?): HrisTab = entries.firstOrNull { it.key == key } ?: HOME
    }
}

/** Maps a logical destination (incl. legacy "leave"/"wfh") to a tab — both land on Schedule. */
private fun logicalTab(key: String): HrisTab = when (key) {
    "leave", "wfh", "timeoff" -> HrisTab.TIME_OFF
    else -> HrisTab.fromKey(key)
}

/** Full-screen pushed destinations (iOS navigation push); they hide the tab-bar/bell/FAB chrome. */
private sealed interface Overlay {
    data class Approvals(val kind: ApprovalKind) : Overlay
    data object Payslips : Overlay
    data object Assets : Overlay
    data object Notifications : Overlay
    data object Announcements : Overlay
    data class AnnouncementDetail(val id: Int, val seed: Announcement?) : Overlay
    data object Tickets : Overlay
    data class TicketDetail(val id: Int) : Overlay
}

/**
 * Bottom-tab host with the iOS chrome: a floating tab-bar pill (Home/Calendar/Schedule/Me) plus a
 * separate Search circle, a notification bell top-right, and a global blue + FAB. Pushed screens
 * live on an overlay back-stack and render full-screen with their own back bar. Mirrors iOS
 * `MainTabView`.
 */
@Composable
fun MainTabScaffold(
    environment: AppEnvironment,
    currentUser: User,
    modifier: Modifier = Modifier,
    initialTab: HrisTab = HrisTab.HOME,
) {
    var selected by rememberSaveable { mutableStateOf(initialTab) }
    var scheduleAdd by remember { mutableStateOf<String?>(null) }
    var fabMenu by remember { mutableStateOf(false) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }

    // Act on deep links (push taps / `hris://` URIs). Mirrors iOS `MainTabView.handle(_:)`.
    val pendingLink by environment.deepLinkRouter.pending.collectAsState()
    LaunchedEffect(pendingLink) {
        val target = pendingLink ?: return@LaunchedEffect
        when (target) {
            is DeepLinkTarget.Tab -> { overlays.clear(); selected = logicalTab(target.key) }
            is DeepLinkTarget.ApplyLeave -> { overlays.clear(); selected = HrisTab.TIME_OFF; scheduleAdd = "apply" }
            is DeepLinkTarget.ScheduleWfh -> { overlays.clear(); selected = HrisTab.TIME_OFF; scheduleAdd = "wfh" }
            is DeepLinkTarget.Notification -> {
                val value = (target.referenceType ?: "").uppercase()
                when {
                    value.contains("TICKET") -> target.referenceId?.let { overlays.clear(); push(Overlay.TicketDetail(it)) }
                    value.contains("LEAVE") -> { overlays.clear(); selected = HrisTab.TIME_OFF; if (currentUser.canApproveLeave) push(Overlay.Approvals(ApprovalKind.LEAVE)) }
                    value.contains("WFH") -> { overlays.clear(); selected = HrisTab.TIME_OFF; if (currentUser.canApproveWfh) push(Overlay.Approvals(ApprovalKind.WFH)) }
                    value.contains("CALENDAR") || value.contains("EVENT") -> { overlays.clear(); selected = HrisTab.CALENDAR }
                    else -> { overlays.clear(); push(Overlay.Notifications) }
                }
            }
        }
        environment.deepLinkRouter.clear()
    }

    // Bell unread-count badge, polled every 30s while foregrounded (mirrors iOS pollUnreadCount).
    val badgeStore = remember { NotificationStore(environment.notificationRepository) }
    val badgeState by badgeStore.state.collectAsState()

    // Once authenticated: request POST_NOTIFICATIONS (API 33+), register the FCM token, poll badge.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching { environment.pushService.registerIfNeeded() }
        while (true) {
            badgeStore.refreshUnreadCount()
            delay(30_000)
        }
    }

    Box(modifier.fillMaxSize()) {
        when (val top = overlays.lastOrNull()) {
            is Overlay.Notifications -> NotificationInboxScreen(
                repository = environment.notificationRepository,
                onBack = ::pop,
                // Reuse the deep-link router so a detail's action routes exactly like a push tap.
                onRoute = { refType, refId -> environment.deepLinkRouter.submit(DeepLinkTarget.Notification(refType, refId)) },
            )
            is Overlay.Approvals -> ApprovalsScreen(
                leaveRepository = environment.leaveRepository,
                wfhRepository = environment.wfhRepository,
                canApproveLeave = currentUser.canApproveLeave,
                canApproveWfh = currentUser.canApproveWfh,
                preselect = top.kind,
                onBack = ::pop,
            )
            is Overlay.Payslips -> PayslipsScreen(environment.payslipRepository, onBack = ::pop)
            is Overlay.Assets -> MyAssetsScreen(environment.assetRepository, onBack = ::pop)
            is Overlay.Announcements -> AnnouncementsScreen(
                repository = environment.announcementRepository,
                onOpen = { push(Overlay.AnnouncementDetail(it.id, it)) },
                onBack = ::pop,
            )
            is Overlay.AnnouncementDetail -> AnnouncementDetailScreen(top.id, top.seed, environment.announcementRepository, onBack = ::pop)
            is Overlay.Tickets -> TicketsScreen(environment.ticketRepository, onOpen = { push(Overlay.TicketDetail(it.id)) }, onBack = ::pop)
            is Overlay.TicketDetail -> TicketDetailScreen(top.id, environment.ticketRepository, onBack = ::pop)
            null -> TabRoot(
                environment = environment,
                currentUser = currentUser,
                selected = selected,
                unreadCount = badgeState.unreadCount,
                onSelect = { selected = it },
                scheduleAdd = scheduleAdd,
                onScheduleAddConsumed = { scheduleAdd = null },
                fabMenu = fabMenu,
                onFabMenu = { fabMenu = it },
                onFabApply = { selected = HrisTab.TIME_OFF; scheduleAdd = "apply" },
                onFabWfh = { selected = HrisTab.TIME_OFF; scheduleAdd = "wfh" },
                onBell = { push(Overlay.Notifications) },
                onOpenApprovals = { push(Overlay.Approvals(it)) },
                onOpenPayslips = { push(Overlay.Payslips) },
                onOpenAssets = { push(Overlay.Assets) },
                onOpenSupport = { push(Overlay.Tickets) },
                onViewAllAnnouncements = { push(Overlay.Announcements) },
                onOpenAnnouncement = { push(Overlay.AnnouncementDetail(it.id, it)) },
            )
        }
    }
}

@Composable
private fun TabRoot(
    environment: AppEnvironment,
    currentUser: User,
    selected: HrisTab,
    unreadCount: Int,
    onSelect: (HrisTab) -> Unit,
    scheduleAdd: String?,
    onScheduleAddConsumed: () -> Unit,
    fabMenu: Boolean,
    onFabMenu: (Boolean) -> Unit,
    onFabApply: () -> Unit,
    onFabWfh: () -> Unit,
    onBell: () -> Unit,
    onOpenApprovals: (ApprovalKind) -> Unit,
    onOpenPayslips: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenSupport: () -> Unit,
    onViewAllAnnouncements: () -> Unit,
    onOpenAnnouncement: (Announcement) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 96.dp)) {
            when (selected) {
                HrisTab.HOME -> HomeScreen(
                    environment = environment,
                    onOpenTimeOff = { onSelect(HrisTab.TIME_OFF) },
                    onOpenCalendar = { onSelect(HrisTab.CALENDAR) },
                    onOpenProfile = { onSelect(HrisTab.ME) },
                    onViewAllAnnouncements = onViewAllAnnouncements,
                    onOpenAnnouncement = onOpenAnnouncement,
                )
                HrisTab.CALENDAR -> CalendarScreen(repository = environment.calendarRepository)
                HrisTab.TIME_OFF -> ScheduleScreen(
                    environment = environment,
                    canApproveLeave = currentUser.canApproveLeave,
                    canApproveWfh = currentUser.canApproveWfh,
                    onOpenApprovals = onOpenApprovals,
                    addAction = scheduleAdd,
                    onAddConsumed = onScheduleAddConsumed,
                )
                HrisTab.ME -> ProfileScreen(
                    environment = environment,
                    onOpenPayslips = onOpenPayslips,
                    onOpenAssets = onOpenAssets,
                    onOpenSupport = onOpenSupport,
                )
                HrisTab.SEARCH -> SearchScreen(environment = environment)
            }
        }

        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Theme.Spacing.lg)) {
            CircleChrome(
                icon = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                onClick = onBell,
            )
            if (unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF3B30),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                ) {
                    Text(
                        if (unreadCount > 99) "99+" else "$unreadCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }

        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Theme.Spacing.lg, bottom = 108.dp)) {
            Surface(onClick = { onFabMenu(true) }, shape = CircleShape, color = Theme.brand, contentColor = Color.White, shadowElevation = 6.dp) {
                Box(Modifier.size(Theme.Size.fab), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, contentDescription = "Add") }
            }
            DropdownMenu(expanded = fabMenu, onDismissRequest = { onFabMenu(false) }) {
                DropdownMenuItem(text = { Text("Apply for Leave") }, onClick = { onFabMenu(false); onFabApply() })
                DropdownMenuItem(text = { Text("Schedule WFH") }, onClick = { onFabMenu(false); onFabWfh() })
            }
        }

        FloatingTabBar(selected = selected, onSelect = onSelect, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Theme.Spacing.md))
    }
}

@Composable
private fun FloatingTabBar(selected: HrisTab, onSelect: (HrisTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(horizontal = Theme.Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 4.dp, modifier = Modifier.weight(1f)) {
            Row(Modifier.padding(horizontal = Theme.Spacing.xs, vertical = Theme.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                HrisTab.MAIN.forEach { tab -> TabItem(tab = tab, selected = selected == tab, onClick = { onSelect(tab) }, modifier = Modifier.weight(1f)) }
            }
        }
        Surface(
            onClick = { onSelect(HrisTab.SEARCH) },
            shape = CircleShape,
            color = if (selected == HrisTab.SEARCH) Theme.brand.copy(alpha = Theme.Opacity.surface) else MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
        ) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = if (selected == HrisTab.SEARCH) Theme.brand else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TabItem(tab: HrisTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (selected) Theme.brand else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (selected) Theme.brand.copy(alpha = Theme.Opacity.fill) else Color.Transparent, RoundedCornerShape(24.dp))
            .padding(vertical = Theme.Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CircleChrome(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 3.dp, modifier = modifier) {
        Box(Modifier.size(Theme.Size.circleButton), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
