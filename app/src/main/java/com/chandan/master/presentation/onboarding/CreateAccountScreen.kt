package com.chandan.master.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.domain.auth.PinError
import com.chandan.master.domain.auth.UsernameError
import com.chandan.master.presentation.components.MasterTextField
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.PinTextField
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.components.ProfileAvatar
import com.chandan.master.presentation.theme.Spacing

@Composable
internal fun usernameErrorText(e: UsernameError?): String? = when (e) {
    null -> null
    UsernameError.EMPTY -> stringResource(R.string.error_username_empty)
    UsernameError.TOO_SHORT -> stringResource(R.string.error_username_short)
    UsernameError.TOO_LONG -> stringResource(R.string.error_username_long)
    UsernameError.INVALID_CHARACTERS -> stringResource(R.string.error_username_chars)
}

@Composable
internal fun pinErrorText(e: PinError?): String? = when (e) {
    null -> null
    PinError.EMPTY -> stringResource(R.string.error_pin_empty)
    PinError.TOO_SHORT -> stringResource(R.string.error_pin_short)
    PinError.TOO_LONG -> stringResource(R.string.error_pin_long)
    PinError.NOT_DIGITS -> stringResource(R.string.error_pin_digits)
}

@Composable
fun CreateAccountScreen(onBack: () -> Unit, viewModel: CreateAccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri.toString())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MasterTopBar(title = stringResource(R.string.create_account_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                ProfileAvatar(size = 72.dp, source = state.avatarPath)
                Column {
                    TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(stringResource(if (state.avatarPath == null) R.string.photo_choose else R.string.photo_change))
                    }
                    if (state.avatarPath != null) {
                        TextButton(onClick = viewModel::onRemovePhoto) { Text(stringResource(R.string.photo_remove)) }
                    }
                }
            }
            if (state.avatarFailed) Text(stringResource(R.string.photo_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.photo_optional), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            MasterTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = stringResource(R.string.field_username),
                error = if (state.showErrors) usernameErrorText(state.usernameError) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            PinTextField(
                value = state.pin,
                onValueChange = viewModel::onPinChange,
                label = stringResource(R.string.field_pin),
                error = if (state.showErrors) pinErrorText(state.pinError) else null,
                hint = if (state.pinIsWeak) stringResource(R.string.hint_pin_weak) else stringResource(R.string.hint_pin_length),
            )
            PinTextField(
                value = state.confirmPin,
                onValueChange = viewModel::onConfirmChange,
                label = stringResource(R.string.field_confirm_pin),
                error = if (state.confirmMismatch && (state.showErrors || state.confirmPin.length >= state.pin.length)) stringResource(R.string.error_pin_mismatch) else null,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.submit() }),
            )
            Text(stringResource(R.string.create_account_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.storageFailed) Text(stringResource(R.string.error_create_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)

            PrimaryButton(
                text = stringResource(if (state.busy) R.string.create_account_busy else R.string.welcome_create_account),
                onClick = { focus.clearFocus(); viewModel.submit() },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
