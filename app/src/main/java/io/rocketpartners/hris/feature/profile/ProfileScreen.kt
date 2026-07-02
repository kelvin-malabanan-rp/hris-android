package io.rocketpartners.hris.feature.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rocketpartners.hris.app.AppEnvironment
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.designsystem.Avatar
import io.rocketpartners.hris.designsystem.ContentCard
import io.rocketpartners.hris.designsystem.DSCard
import io.rocketpartners.hris.designsystem.ErrorState
import io.rocketpartners.hris.designsystem.SkeletonBlock
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.UserProfile
import kotlinx.coroutines.launch

/**
 * The "Me" tab: profile header, details/address/emergency cards, a services hub (Payslips, My
 * Assets, Support), and edit/password/sign-out actions. Mirrors iOS `ProfileView`.
 */
@Composable
fun ProfileScreen(
    environment: AppEnvironment,
    modifier: Modifier = Modifier,
    onOpenPayslips: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenSupport: () -> Unit,
) {
    val store = remember { ProfileStore(environment.profileRepository) }
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { store.load() }

    Box(modifier.fillMaxSize()) {
        when (val phase = state.phase) {
            is Phase.Failed -> ErrorState(message = phase.message, title = "Couldn't load profile", retry = { store.load() })
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
            ) {
                io.rocketpartners.hris.designsystem.ScreenHeader("Me")
                val profile = state.profile
                if (profile == null) {
                    SkeletonBlock(Modifier.fillMaxWidth(), height = 88.dp)
                    SkeletonBlock(Modifier.fillMaxWidth(), height = 160.dp)
                } else {
                    HeaderCard(profile)
                    DetailsCard(profile)
                    profile.formattedAddress?.let { AddressCard(it) }
                    if (profile.hasEmergencyContact) EmergencyCard(profile)
                }
                ServicesCard(onOpenPayslips, onOpenAssets, onOpenSupport)
                ActionsCard(
                    onEdit = { showEdit = true },
                    onChangePassword = { showPassword = true },
                    onSignOut = { showSignOut = true },
                )
            }
        }
    }

    if (showEdit) {
        state.profile?.let { EditProfileSheet(store = store, current = it, onDismiss = { showEdit = false }) }
    }
    if (showPassword) {
        ChangePasswordSheet(store = store, onDismiss = { showPassword = false })
    }
    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Sign out of HRIS?") },
            confirmButton = { TextButton(onClick = { showSignOut = false; scope.launch { environment.authService.logout() } }) { Text("Sign Out") } },
            dismissButton = { TextButton(onClick = { showSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun HeaderCard(profile: UserProfile) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            Avatar(name = profile.displayName, model = profile.profileImageUrl, size = 64.dp)
            Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.xxs)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                profile.positionTitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                profile.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun DetailsCard(profile: UserProfile) {
    DSCard(title = "Details") {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            profile.phone?.let { DetailRow(Icons.Filled.Phone, "Phone", it) }
            profile.employeeId?.let { DetailRow(Icons.Filled.Badge, "Employee ID", it) }
            profile.departmentName?.let { DetailRow(Icons.Filled.Business, "Department", it) }
            profile.positionTitle?.let { DetailRow(Icons.Filled.Work, "Position", it) }
        }
    }
}

@Composable
private fun AddressCard(address: String) {
    DSCard(title = "Address") {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(Theme.Size.iconInline))
            Text(address, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmergencyCard(profile: UserProfile) {
    DSCard(title = "Emergency Contact") {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            profile.emergencyContactName?.takeIf { it.isNotEmpty() }?.let { DetailRow(Icons.Filled.Person, "Name", it) }
            profile.emergencyContactRelationship?.takeIf { it.isNotEmpty() }?.let { DetailRow(Icons.Filled.Favorite, "Relationship", it) }
            profile.emergencyContactPhone?.takeIf { it.isNotEmpty() }?.let { DetailRow(Icons.Filled.Phone, "Phone", it) }
            profile.emergencyContactMobile?.takeIf { it.isNotEmpty() }?.let { DetailRow(Icons.Filled.Smartphone, "Mobile", it) }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        Icon(icon, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(Theme.Size.iconInline))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun ServicesCard(onOpenPayslips: () -> Unit, onOpenAssets: () -> Unit, onOpenSupport: () -> Unit) {
    DSCard(title = "Services") {
        Column {
            ServiceLink(Icons.Filled.Description, "Payslips", onOpenPayslips)
            HorizontalDivider(Modifier.padding(start = Theme.Spacing.xl))
            ServiceLink(Icons.Filled.Inventory2, "My Assets", onOpenAssets)
            HorizontalDivider(Modifier.padding(start = Theme.Spacing.xl))
            ServiceLink(Icons.Filled.Home, "Support", onOpenSupport)
        }
    }
}

@Composable
private fun ServiceLink(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Theme.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = Theme.brand, modifier = Modifier.size(Theme.Size.iconInline))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionsCard(onEdit: () -> Unit, onChangePassword: () -> Unit, onSignOut: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Edit Profile", modifier = Modifier.padding(start = Theme.Spacing.sm))
        }
        OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Change Password", modifier = Modifier.padding(start = Theme.Spacing.sm))
        }
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Sign Out", modifier = Modifier.padding(start = Theme.Spacing.sm))
        }
    }
}
