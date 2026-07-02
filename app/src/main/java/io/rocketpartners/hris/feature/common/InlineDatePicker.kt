package io.rocketpartners.hris.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A date picker rendered **inline** (not in a `Dialog` window) so it can be used inside a
 * `ModalBottomSheet` without the two fighting over focus/scrim and dismissing the sheet. Dates are
 * handled in UTC to match the wire-date civil-day convention. Callers swap this in place of the
 * sheet's form while picking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineDatePicker(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "OK",
) {
    val state = rememberDatePickerStateUtc(initial)
    Column(modifier) {
        DatePicker(state = state, showModeToggle = false)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()) else onCancel()
            }) { Text(confirmLabel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDatePickerStateUtc(initial: LocalDate) =
    androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
