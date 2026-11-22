package com.chandan.master.presentation.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chandan.master.R
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import com.chandan.master.presentation.components.SectionHeader
import com.chandan.master.presentation.theme.MasterPalette
import com.chandan.master.presentation.theme.MasterTheme
import com.chandan.master.presentation.theme.Spacing

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
private fun AccentPalette.labelRes(): Int = when (this) {
    AccentPalette.VERMILION -> R.string.accent_vermilion
    AccentPalette.BURNT_ORANGE -> R.string.accent_burnt_orange
    AccentPalette.AMBER -> R.string.accent_amber
    AccentPalette.CRIMSON -> R.string.accent_crimson
    AccentPalette.RUST -> R.string.accent_rust
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSection(
    preferences: AppPreferences,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionHeader(stringResource(R.string.profile_section_appearance))

        Text(stringResource(R.string.profile_theme), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        val modes = ThemeMode.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = preferences.themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) { Text(stringResource(mode.labelRes())) }
            }
        }

        Text(stringResource(R.string.profile_accent), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AccentPalette.entries.forEach { palette ->
                AccentSwatch(
                    palette = palette,
                    selected = preferences.accentPalette == palette,
                    onClick = { onAccentSelected(palette) },
                )
            }
        }
        // Colour is never the only cue: the selected accent is also named in text.
        Text(
            stringResource(preferences.accentPalette.labelRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccentSwatch(palette: AccentPalette, selected: Boolean, onClick: () -> Unit) {
    val color = Color(MasterPalette.accent(palette, MasterTheme.isDark).primary)
    val onColor = Color(MasterPalette.accent(palette, MasterTheme.isDark).onPrimary)
    val name = stringResource(palette.labelRes())
    val description = if (selected) stringResource(R.string.accent_selected, name) else name
    Box(
        modifier = Modifier
            .size(Spacing.minTouchTarget)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = color,
            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground) else null,
        ) {
            if (selected) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = onColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
