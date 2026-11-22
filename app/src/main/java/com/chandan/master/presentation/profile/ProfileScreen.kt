package com.chandan.master.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.ProfileAvatar
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.openExternalLink

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noBrowserMessage = stringResource(R.string.developer_no_browser)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MasterTopBar(title = stringResource(R.string.nav_profile), onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.screenHorizontal, vertical = Spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            // 1. Account information
            item { ProfileHeader(username = state.username, avatarPath = state.avatarPath) }

            // 1b. Edit profile, account security, lock
            item {
                Column {
                    SettingsRow(Icons.Outlined.Person, stringResource(R.string.edit_profile_title), onClick = onEditProfile)
                    SettingsRow(Icons.Outlined.Shield, stringResource(R.string.security_title), stringResource(R.string.profile_security_subtitle), onClick = onOpenSecurity)
                    SettingsRow(Icons.Outlined.Lock, stringResource(R.string.security_lock_now), onClick = viewModel::lockNow)
                    SettingsRow(Icons.Outlined.Notifications, stringResource(R.string.notifications_title), onClick = onOpenNotifications)
                    SettingsRow(Icons.Outlined.Label, stringResource(R.string.categories_title), stringResource(R.string.profile_categories_subtitle), onClick = onOpenCategories)
                    SettingsRow(Icons.Outlined.Backup, stringResource(R.string.backup_title), stringResource(R.string.profile_backup_subtitle), onClick = onOpenBackup)
                    SettingsRow(Icons.Outlined.Info, stringResource(R.string.about_title), stringResource(R.string.profile_about_subtitle), onClick = onOpenAbout)
                }
            }

            // 2. Appearance (working: persisted in DataStore)
            item {
                AppearanceSection(
                    preferences = state.preferences,
                    onThemeModeSelected = viewModel::onThemeModeSelected,
                    onAccentSelected = viewModel::onAccentSelected,
                )
            }

            // Any new settings go ABOVE this point. Developer & Social Links must stay last.

            // 3. Developer & Social Links: always the final section, separate from account/security.
            item {
                Spacer(Modifier.height(Spacing.xl))
                DeveloperLinksSection(
                    onOpenLink = { url ->
                        if (!context.openExternalLink(url)) {
                            Toast.makeText(context, noBrowserMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(username: String?, avatarPath: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ProfileAvatar(size = 112.dp, source = avatarPath)
        Text(
            text = username ?: stringResource(R.string.profile_guest_label),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
