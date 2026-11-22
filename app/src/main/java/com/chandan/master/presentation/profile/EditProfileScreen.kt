package com.chandan.master.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.MasterTextField
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.components.ProfileAvatar
import com.chandan.master.presentation.onboarding.usernameErrorText
import com.chandan.master.presentation.theme.Spacing

@Composable
fun EditProfileScreen(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri.toString())
    }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MasterTopBar(title = stringResource(R.string.edit_profile_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ProfileAvatar(size = 112.dp, source = state.avatarPath)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Text(stringResource(if (state.avatarPath == null) R.string.photo_choose else R.string.photo_change))
                }
                if (state.avatarPath != null) {
                    TextButton(onClick = viewModel::onRestoreDefault) { Text(stringResource(R.string.photo_restore_default)) }
                }
            }
            if (state.photoFailed) Text(stringResource(R.string.photo_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            MasterTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = stringResource(R.string.field_username),
                error = usernameErrorText(state.usernameError).takeIf { state.showError || state.username.isNotEmpty() && state.usernameError != null },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (state.canSave) viewModel.save() }),
            )
            PrimaryButton(stringResource(R.string.action_save), onClick = viewModel::save, enabled = state.canSave, modifier = Modifier.fillMaxWidth())
        }
    }
}
