package com.chandan.master.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chandan.master.BuildConfig
import com.chandan.master.R
import com.chandan.master.presentation.components.BrandWordmark
import com.chandan.master.presentation.components.MasterTopBar
import com.chandan.master.presentation.components.ProfileAvatar
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.theme.Spacing
import com.chandan.master.presentation.util.openExternalLink

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val noBrowser = stringResource(R.string.developer_no_browser)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { MasterTopBar(stringResource(R.string.about_title), onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ProfileAvatar(size = 96.dp)
            BrandWordmark()
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)

            Column(Modifier.fillMaxWidth()) {
                SectionHeader(stringResource(R.string.about_privacy_title))
                Text(stringResource(R.string.about_privacy_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.fillMaxWidth()) {
                SectionHeader(stringResource(R.string.about_ack_title))
                Text(stringResource(R.string.about_ack_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DeveloperLinksSection(onOpenLink = { url -> if (!context.openExternalLink(url)) Toast.makeText(context, noBrowser, Toast.LENGTH_SHORT).show() })
        }
    }
}
