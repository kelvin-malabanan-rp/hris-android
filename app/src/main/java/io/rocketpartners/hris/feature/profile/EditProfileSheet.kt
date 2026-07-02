package io.rocketpartners.hris.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.rocketpartners.hris.designsystem.Theme
import io.rocketpartners.hris.model.UserProfile
import kotlinx.coroutines.launch

/** Bottom sheet to edit the mutable profile fields (partial PATCH). Mirrors iOS `EditProfileView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(store: ProfileStore, current: UserProfile, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf(current.firstName ?: "") }
    var lastName by remember { mutableStateOf(current.lastName ?: "") }
    var phone by remember { mutableStateOf(current.phone ?: "") }
    var personalEmail by remember { mutableStateOf(current.personalEmail ?: "") }
    var city by remember { mutableStateOf(current.city ?: "") }
    var country by remember { mutableStateOf(current.country ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Field("First name", firstName) { firstName = it }
            Field("Last name", lastName) { lastName = it }
            Field("Phone", phone) { phone = it }
            Field("Personal email", personalEmail) { personalEmail = it }
            Field("City", city) { city = it }
            Field("Country", country) { country = it }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                enabled = !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        val message = store.save(
                            ProfileUpdate(
                                firstName = firstName.ifBlank { null },
                                lastName = lastName.ifBlank { null },
                                phone = phone.ifBlank { null },
                                personalEmail = personalEmail.ifBlank { null },
                                city = city.ifBlank { null },
                                country = country.ifBlank { null },
                            ),
                        )
                        saving = false
                        if (message == null) onDismiss() else error = message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Changes") }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
