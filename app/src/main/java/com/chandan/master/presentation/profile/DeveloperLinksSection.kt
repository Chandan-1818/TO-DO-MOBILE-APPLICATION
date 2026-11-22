package com.chandan.master.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import com.chandan.master.R
import com.chandan.master.core.constants.ExternalLinks
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.theme.Spacing

/**
 * "Developer & Social Links": GitHub and LinkedIn. Stateless; the caller opens the URL in a browser.
 * Rendered as the last section of the Profile page, separate from account and security settings.
 */
@Composable
fun DeveloperLinksSection(onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionHeader(stringResource(R.string.developer_links_title), Modifier.padding(top = Spacing.sm))
        DeveloperLinkRow(
            icon = Icons.Outlined.Code,
            title = stringResource(R.string.developer_github),
            display = stringResource(R.string.developer_github_display),
            onClick = { onOpenLink(ExternalLinks.GITHUB_URL) },
        )
        DeveloperLinkRow(
            icon = Icons.Outlined.Work,
            title = stringResource(R.string.developer_linkedin),
            display = stringResource(R.string.developer_linkedin_display),
            onClick = { onOpenLink(ExternalLinks.LINKEDIN_URL) },
        )
    }
}

@Composable
private fun DeveloperLinkRow(icon: ImageVector, title: String, display: String, onClick: () -> Unit) {
    val hint = stringResource(R.string.developer_open_link)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget + Spacing.sm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $display. $hint" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(display, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
