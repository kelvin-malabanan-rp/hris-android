package io.rocketpartners.hris.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.rocketpartners.hris.designsystem.Theme
import kotlinx.coroutines.launch

/** Bottom sheet to change the account password. Mirrors iOS `ChangePasswordView`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordSheet(store: ProfileStore, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val mismatch = confirm.isNotEmpty() && new != confirm
    val canSubmit = current.isNotEmpty() && new.length >= 8 && new == confirm && !saving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.xl).padding(bottom = Theme.Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            Text("Change Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Secure("Current password", current) { current = it }
            Secure("New password", new) { new = it }
            Secure("Confirm new password", confirm) { confirm = it }
            if (mismatch) Text("Passwords don't match.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                enabled = canSubmit,
                onClick = {
                    scope.launch {
                        saving = true
                        val message = store.changePassword(current, new, confirm)
                        saving = false
                        if (message == null) onDismiss() else error = message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Update Password") }
        }
    }
}

@Composable
private fun Secure(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}
