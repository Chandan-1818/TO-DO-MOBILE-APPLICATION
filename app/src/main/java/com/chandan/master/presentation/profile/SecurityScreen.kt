package com.chandan.master.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.auth.AutoLockDelay
import com.chandan.master.presentation.auth.BiometricAvailability
import com.chandan.master.presentation.auth.BiometricHelper
import com.chandan.master.presentation.components.DangerButtonColors
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.PinTextField
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.components.SecondaryButton
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.onboarding.pinErrorText
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.findFragmentActivity

@Composable
private fun AutoLockDelay.label(): String = stringResource(
    when (this) {
        AutoLockDelay.IMMEDIATELY -> R.string.autolock_immediately
        AutoLockDelay.SECONDS_30 -> R.string.autolock_30s
        AutoLockDelay.MINUTE_1 -> R.string.autolock_1m
        AutoLockDelay.MINUTES_5 -> R.string.autolock_5m
    },
)

@Composable
fun SecurityScreen(onBack: () -> Unit, viewModel: SecurityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val availability = remember { BiometricHelper.availability(context) }
    var showReset by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val promptTitle = stringResource(R.string.biometric_enable_title)
    val promptNegative = stringResource(R.string.action_cancel)

    fun enableBiometric() {
        val host = activity ?: return
        val cipher = viewModel.prepareBiometricEnrollment() ?: return
        BiometricHelper.authenticate(
            host, cipher, promptTitle, promptNegative,
            onSuccess = viewModel::onBiometricEnrolled,
            onFailure = { f -> viewModel.onBiometricEnrollFailed(showNotice = f != com.chandan.master.presentation.auth.BiometricFailure.CANCELED) },
        )
    }

    // Clear the form after a successful change.
    LaunchedEffect(state.pinStatus) {
        if (state.pinStatus == PinChangeStatus.Success) { current = ""; newPin = ""; confirm = "" }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MasterTopBar(title = stringResource(R.string.security_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            // ---- Biometric unlock
            SectionHeader(stringResource(R.string.security_unlock_section))
            val canEnable = availability == BiometricAvailability.AVAILABLE
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(
                    value = state.biometricEnabled, enabled = canEnable || state.biometricEnabled, role = Role.Switch,
                    onValueChange = { on -> if (on) enableBiometric() else viewModel.disableBiometric() },
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.security_biometric), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(
                            when {
                                availability == BiometricAvailability.AVAILABLE -> R.string.security_biometric_desc
                                availability == BiometricAvailability.NO_HARDWARE -> R.string.biometric_no_hardware
                                availability == BiometricAvailability.NONE_ENROLLED -> R.string.biometric_none_enrolled
                                availability == BiometricAvailability.TEMPORARILY_UNAVAILABLE -> R.string.biometric_unavailable_now
                                else -> R.string.biometric_unsupported
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.biometricEnabled, onCheckedChange = null, enabled = canEnable || state.biometricEnabled)
            }
            when (state.biometricNotice) {
                BiometricNotice.ENROLL_FAILED -> Text(stringResource(R.string.biometric_enroll_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                BiometricNotice.CHANGED -> Text(stringResource(R.string.lock_biometric_changed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                BiometricNotice.NONE -> Unit
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Auto-lock
            SectionHeader(stringResource(R.string.security_autolock_section))
            Column(Modifier.fillMaxWidth()) {
                AutoLockDelay.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = state.autoLockDelay == option, role = Role.RadioButton, onClick = { viewModel.onAutoLockSelected(option) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.autoLockDelay == option, onClick = null)
                        Text(option.label(), modifier = Modifier.padding(start = Spacing.md), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Text(stringResource(R.string.security_autolock_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SecondaryButton(stringResource(R.string.security_lock_now), onClick = viewModel::lockNow, modifier = Modifier.fillMaxWidth())

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Change PIN
            SectionHeader(stringResource(R.string.security_change_pin))
            val statusText: String? = when (val s = state.pinStatus) {
                PinChangeStatus.Idle, PinChangeStatus.Success -> null
                is PinChangeStatus.WrongCurrent -> if (s.attemptsLeft > 0) pluralStringResource(R.plurals.lock_wrong_pin, s.attemptsLeft, s.attemptsLeft) else stringResource(R.string.lock_wrong_pin_no_left)
                is PinChangeStatus.LockedOut -> stringResource(R.string.lock_try_again_in, s.seconds)
                is PinChangeStatus.InvalidNew -> pinErrorText(s.error)
                PinChangeStatus.Mismatch -> stringResource(R.string.error_pin_mismatch)
                PinChangeStatus.Unavailable -> stringResource(R.string.lock_credentials_unavailable)
                PinChangeStatus.Failed -> stringResource(R.string.error_pin_change_failed)
            }
            PinTextField(current, { current = it; viewModel.clearPinStatus() }, stringResource(R.string.field_current_pin))
            PinTextField(newPin, { newPin = it; viewModel.clearPinStatus() }, stringResource(R.string.field_new_pin), hint = stringResource(R.string.hint_pin_length))
            PinTextField(confirm, { confirm = it; viewModel.clearPinStatus() }, stringResource(R.string.field_confirm_new_pin), imeAction = androidx.compose.ui.text.input.ImeAction.Done)
            if (statusText != null) Text(statusText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (state.pinStatus == PinChangeStatus.Success) Text(stringResource(R.string.security_pin_changed), color = com.chandan.master.presentation.theme.MasterTheme.extra.success, style = MaterialTheme.typography.bodyMedium)
            PrimaryButton(
                stringResource(R.string.security_change_pin_action),
                onClick = {
                    viewModel.changePin(current, newPin, confirm)
                },
                enabled = !state.busy && current.isNotEmpty() && newPin.isNotEmpty() && confirm.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---- Reset
            SectionHeader(stringResource(R.string.security_danger_section))
            Text(stringResource(R.string.security_reset_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { showReset = true }, colors = DangerButtonColors(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.security_reset_action))
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text(stringResource(R.string.reset_message)) },
            confirmButton = {
                TextButton(onClick = { showReset = false; viewModel.resetAccount() }, colors = DangerButtonColors()) { Text(stringResource(R.string.reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
