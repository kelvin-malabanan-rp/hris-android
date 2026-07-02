package io.rocketpartners.hris.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.feature.assets.MyAssetsScreen
import io.rocketpartners.hris.feature.calendar.CalendarScreen
import io.rocketpartners.hris.feature.home.HomeScreen
import io.rocketpartners.hris.feature.payslips.PayslipsScreen
import io.rocketpartners.hris.feature.profile.ProfileScreen
import io.rocketpartners.hris.feature.search.SearchScreen
import io.rocketpartners.hris.feature.timeoff.ApprovalKind
import io.rocketpartners.hris.feature.timeoff.ApprovalsScreen
import io.rocketpartners.hris.feature.timeoff.ScheduleScreen
import io.rocketpartners.hris.model.User

/** The five destinations. The first four live in the tab-bar pill; [SEARCH] is a separate circle. */
enum class HrisTab(val key: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.GridView),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    TIME_OFF("leave", "Schedule", Icons.Outlined.CalendarViewWeek),
    ME("me", "Me", Icons.Filled.Person),
    SEARCH("search", "Search", Icons.Filled.Search);

    companion object {
        val MAIN = listOf(HOME, CALENDAR, TIME_OFF, ME)
        fun fromKey(key: String?): HrisTab = entries.firstOrNull { it.key == key } ?: HOME
    }
}

/**
 * Bottom-tab host with the iOS chrome: a floating tab-bar pill (Home/Calendar/Schedule/Me) plus a
 * separate Search circle, a notification bell top-right, and a global blue + FAB. Sub-screens
 * (approvals, payslips, assets) render full-screen with their own back bar, hiding the chrome.
 * Mirrors iOS `MainTabView`.
 */
@Composable
fun MainTabScaffold(
    environment: AppEnvironment,
    currentUser: User,
    modifier: Modifier = Modifier,
    initialTab: HrisTab = HrisTab.HOME,
) {
    var selected by rememberSaveable { mutableStateOf(initialTab) }
    var timeOffApprovals by rememberSaveable { mutableStateOf<ApprovalKind?>(null) }
    var meRoute by rememberSaveable { mutableStateOf("root") }
    var scheduleAdd by remember { mutableStateOf<String?>(null) }
    var fabMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Full-screen sub-navigation (iOS push): hides the tab-bar/bell/FAB chrome.
    val subScreen: (@Composable () -> Unit)? = when {
        selected == HrisTab.TIME_OFF && timeOffApprovals != null -> {
            {
                ApprovalsScreen(
                    leaveRepository = environment.leaveRepository,
                    wfhRepository = environment.wfhRepository,
                    canApproveLeave = currentUser.canApproveLeave,
                    canApproveWfh = currentUser.canApproveWfh,
                    preselect = timeOffApprovals!!,
                    onBack = { timeOffApprovals = null },
                )
            }
        }
        selected == HrisTab.ME && meRoute == "payslips" -> {
            { PayslipsScreen(environment.payslipRepository, onBack = { meRoute = "root" }) }
        }
        selected == HrisTab.ME && meRoute == "assets" -> {
            { MyAssetsScreen(environment.assetRepository, onBack = { meRoute = "root" }) }
        }
        else -> null
    }

    Box(modifier.fillMaxSize()) {
        if (subScreen != null) {
            subScreen()
        } else {
            // Tab root content, inset at the bottom so it clears the floating bar.
            Box(Modifier.fillMaxSize().padding(bottom = 96.dp)) {
                when (selected) {
                    HrisTab.HOME -> HomeScreen(
                        environment = environment,
                        onOpenTimeOff = { selected = HrisTab.TIME_OFF },
                        onOpenCalendar = { selected = HrisTab.CALENDAR },
                        onOpenProfile = { selected = HrisTab.ME },
                    )
                    HrisTab.CALENDAR -> CalendarScreen(repository = environment.calendarRepository)
                    HrisTab.TIME_OFF -> ScheduleScreen(
                        environment = environment,
                        canApproveLeave = currentUser.canApproveLeave,
                        canApproveWfh = currentUser.canApproveWfh,
                        onOpenApprovals = { timeOffApprovals = it },
                        addAction = scheduleAdd,
                        onAddConsumed = { scheduleAdd = null },
                    )
                    HrisTab.ME -> ProfileScreen(
                        environment = environment,
                        onOpenPayslips = { meRoute = "payslips" },
                        onOpenAssets = { meRoute = "assets" },
                        onOpenSupport = { selected = HrisTab.SEARCH },
                    )
                    HrisTab.SEARCH -> SearchScreen()
                }
            }

            // Notification bell (top-right).
            CircleChrome(
                icon = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                onClick = { Toast.makeText(context, "Notifications coming soon", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Theme.Spacing.lg),
            )

            // Global add FAB (above the bar).
            Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = Theme.Spacing.lg, bottom = 108.dp)) {
                Surface(
                    onClick = { fabMenu = true },
                    shape = CircleShape,
                    color = Theme.brand,
                    contentColor = Color.White,
                    shadowElevation = 6.dp,
                ) {
                    Box(Modifier.size(Theme.Size.fab), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
                DropdownMenu(expanded = fabMenu, onDismissRequest = { fabMenu = false }) {
                    DropdownMenuItem(text = { Text("Apply for Leave") }, onClick = { fabMenu = false; selected = HrisTab.TIME_OFF; scheduleAdd = "apply" })
                    DropdownMenuItem(text = { Text("Schedule WFH") }, onClick = { fabMenu = false; selected = HrisTab.TIME_OFF; scheduleAdd = "wfh" })
                }
            }

            FloatingTabBar(
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Theme.Spacing.md),
            )
        }
    }
}

@Composable
private fun FloatingTabBar(selected: HrisTab, onSelect: (HrisTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(horizontal = Theme.Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
            modifier = Modifier.weight(1f),
        ) {
            Row(Modifier.padding(horizontal = Theme.Spacing.xs, vertical = Theme.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                HrisTab.MAIN.forEach { tab ->
                    TabItem(tab = tab, selected = selected == tab, onClick = { onSelect(tab) }, modifier = Modifier.weight(1f))
                }
            }
        }
        // Search as a standalone circle.
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
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
        modifier = modifier,
    ) {
        Box(Modifier.size(Theme.Size.circleButton), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
