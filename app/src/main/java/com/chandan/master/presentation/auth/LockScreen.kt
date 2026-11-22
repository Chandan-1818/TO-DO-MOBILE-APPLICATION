package com.chandan.master.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.PinDots
import com.chandan.master.presentation.components.PinPad
import com.chandan.master.presentation.components.ProfileAvatar
import com.chandan.master.presentation.components.DangerButtonColors
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.findFragmentActivity

/** Full-screen PIN / biometric lock. Drawn over the app; it swallows all touches so nothing underneath is reachable. */
@Composable
fun LockScreen(avatarSource: String?, viewModel: LockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val availability = remember { BiometricHelper.availability(context) }
    val canUseBiometric = state.biometricEnabled && availability == BiometricAvailability.AVAILABLE
    var showReset by remember { mutableStateOf(false) }
    var autoPrompted by remember { mutableStateOf(false) }
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptNegative = stringResource(R.string.biometric_use_pin)

    fun launchBiometric() {
        val host = activity ?: return
        val cipher = viewModel.biometricCipher() ?: return
        BiometricHelper.authenticate(host, cipher, promptTitle, promptNegative, viewModel::onBiometricSuccess, viewModel::onBiometricFailure)
    }

    // Offer biometrics once, as soon as the app is actually in the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(canUseBiometric) {
        if (!canUseBiometric) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!autoPrompted) { autoPrompted = true; launchBiometric() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { /* consume touches */ },
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        ) {
            ProfileAvatar(size = 72.dp, source = avatarSource)
            Text(
                stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() },
            )
            PinDots(length = state.pinLength, filled = state.enteredCount, isError = state.error is LockError.WrongPin)

            val message = lockMessage(state)
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error is LockError.WrongPin || state.error == LockError.CredentialsUnavailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.lg).semantics { liveRegion = LiveRegionMode.Polite },
            )

            PinPad(
                onDigit = viewModel::onDigit,
                onDelete = viewModel::onDelete,
                enabled = state.pinLength > 0 && !state.busy && state.lockedSecondsLeft == 0 && state.error != LockError.CredentialsUnavailable,
                bottomStart = {
                    if (canUseBiometric) {
                        val label = stringResource(R.string.biometric_unlock)
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).clickable(role = Role.Button, onClick = { launchBiometric() }),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Fingerprint, contentDescription = label, tint = MaterialTheme.colorScheme.primary) }
                    } else Box(Modifier.size(72.dp))
                },
            )
            TextButton(onClick = { showReset = true }) { Text(stringResource(R.string.lock_forgot_pin)) }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text(stringResource(R.string.reset_message)) },
            confirmButton = {
                TextButton(onClick = { showReset = false; viewModel.resetAccount() }, colors = DangerButtonColors()) {
                    Text(stringResource(R.string.reset_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun lockMessage(state: LockUiState): String? = when (val e = state.error) {
    is LockError.WrongPin -> if (e.attemptsLeft > 0) pluralStringResource(R.plurals.lock_wrong_pin, e.attemptsLeft, e.attemptsLeft) else stringResource(R.string.lock_wrong_pin_no_left)
    LockError.TooManyAttempts -> stringResource(R.string.lock_try_again_in, state.lockedSecondsLeft)
    LockError.BiometricChanged -> stringResource(R.string.lock_biometric_changed)
    LockError.BiometricLockedOut -> stringResource(R.string.lock_biometric_locked_out)
    LockError.CredentialsUnavailable -> stringResource(R.string.lock_credentials_unavailable)
    null -> stringResource(R.string.lock_enter_pin)
}
