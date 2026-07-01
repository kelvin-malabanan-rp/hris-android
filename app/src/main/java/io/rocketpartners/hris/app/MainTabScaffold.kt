package io.rocketpartners.hris.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.User

/** The five bottom-navigation destinations, mirroring the iOS `MainTabView` tabs. */
enum class HrisTab(val key: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    TIME_OFF("leave", "Time Off", Icons.Outlined.BeachAccess),
    ME("me", "Me", Icons.Filled.Person),
    SEARCH("search", "Search", Icons.Filled.Search);

    companion object {
        fun fromKey(key: String?): HrisTab = entries.firstOrNull { it.key == key } ?: HOME
    }
}

/**
 * Bottom-tab host. Each tab's real screen is filled in during its feature phase; until then a
 * labelled placeholder renders so the shell is navigable end-to-end. Mirrors iOS `MainTabView`.
 */
@Composable
fun MainTabScaffold(
    environment: AppEnvironment,
    currentUser: User,
    modifier: Modifier = Modifier,
    initialTab: HrisTab = HrisTab.HOME,
) {
    var selected by rememberSaveable { mutableStateOf(initialTab) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                HrisTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            TabPlaceholder(tab = selected, userName = currentUser.name)
        }
    }
}

@Composable
private fun TabPlaceholder(tab: HrisTab, userName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
    ) {
        Icon(tab.icon, contentDescription = null, tint = Theme.brand)
        Text(tab.label, textAlign = TextAlign.Center)
        Text(
            "Signed in as $userName",
            textAlign = TextAlign.Center,
        )
    }
}
