package com.chandan.master.presentation.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chandan.master.R
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.rememberNotificationPermissionRequest
import com.chandan.master.presentation.theme.Spacing

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val p by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val askPermission = rememberNotificationPermissionRequest()
    val systemAllows = NotificationManagerCompat.from(context).areNotificationsEnabled()

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { MasterTopBar(stringResource(R.string.notifications_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SwitchRow(stringResource(R.string.notifications_master), stringResource(R.string.notifications_master_desc), p.notificationsEnabled) { on -> viewModel.setMaster(on); if (on) askPermission() }
            SwitchRow(stringResource(R.string.notifications_tasks), null, p.taskReminders, enabled = p.notificationsEnabled) { viewModel.setTasks(it) }
            SwitchRow(stringResource(R.string.notifications_habits), null, p.habitReminders, enabled = p.notificationsEnabled) { viewModel.setHabits(it) }
            SwitchRow(stringResource(R.string.notifications_focus), null, p.focusNotifications, enabled = p.notificationsEnabled) { viewModel.setFocus(it) }

            if (!systemAllows) {
                Text(stringResource(R.string.notifications_blocked), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.md))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.notifications_open_system)) }
            }
            Text(stringResource(R.string.notifications_best_effort), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.md))
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
