package com.chandan.master.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.chandan.master.domain.model.AccentPalette

/** Colours that Material 3's ColorScheme has no slot for. */
@Immutable
data class MasterExtraColors(
    val success: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

private val LocalExtraColors = staticCompositionLocalOf {
    toExtra(MasterPalette.darkSemantic)
}
private val LocalDarkTheme = staticCompositionLocalOf { true }

object MasterTheme {
    val extra: MasterExtraColors
        @Composable @ReadOnlyComposable get() = LocalExtraColors.current

    /** Effective dark/light state after applying the user's theme preference. */
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalDarkTheme.current

    val spacing: Spacing = Spacing
}

private fun c(value: Long) = Color(value)

private fun toExtra(s: SemanticColors) = MasterExtraColors(
    success = c(s.success), danger = c(s.danger), onDanger = c(s.onDanger),
    dangerContainer = c(s.dangerContainer), onDangerContainer = c(s.onDangerContainer),
)

/** Every ColorScheme slot is set explicitly so no Material baseline (purple/blue) colour can leak in. */
internal fun buildColorScheme(darkTheme: Boolean, accent: AccentPalette): ColorScheme {
    val n = if (darkTheme) MasterPalette.dark else MasterPalette.light
    val a = MasterPalette.accent(accent, darkTheme)
    val s = if (darkTheme) MasterPalette.darkSemantic else MasterPalette.lightSemantic
    val inversePrimary = MasterPalette.accent(accent, !darkTheme).primary
    val primary = c(a.primary)
    val onPrimary = c(a.onPrimary)
    val primaryContainer = c(a.primaryContainer)
    val onPrimaryContainer = c(a.onPrimaryContainer)
    return if (darkTheme) darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = c(inversePrimary),
        secondary = c(n.secondary), onSecondary = c(n.onSecondary),
        secondaryContainer = c(n.surfaceHighest), onSecondaryContainer = c(n.onSurface),
        tertiary = c(n.secondary), onTertiary = c(n.onSecondary),
        tertiaryContainer = c(n.surfaceHighest), onTertiaryContainer = c(n.onSurface),
        background = c(n.background), onBackground = c(n.onBackground),
        surface = c(n.surface), onSurface = c(n.onSurface),
        surfaceVariant = c(n.surfaceVariant), onSurfaceVariant = c(n.onSurfaceVariant),
        surfaceTint = c(a.primary),
        inverseSurface = c(n.inverseSurface), inverseOnSurface = c(n.inverseOnSurface),
        error = c(s.danger), onError = c(s.onDanger),
        errorContainer = c(s.dangerContainer), onErrorContainer = c(s.onDangerContainer),
        outline = c(n.outline), outlineVariant = c(n.outlineVariant), scrim = Color.Black,
        surfaceBright = c(n.surfaceHighest), surfaceDim = c(n.surfaceLowest),
        surfaceContainerLowest = c(n.surfaceLowest), surfaceContainerLow = c(n.surfaceLow),
        surfaceContainer = c(n.surface), surfaceContainerHigh = c(n.surfaceHigh),
        surfaceContainerHighest = c(n.surfaceHighest),
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        inversePrimary = c(inversePrimary),
        secondary = c(n.secondary), onSecondary = c(n.onSecondary),
        secondaryContainer = c(n.surfaceHighest), onSecondaryContainer = c(n.onSurface),
        tertiary = c(n.secondary), onTertiary = c(n.onSecondary),
        tertiaryContainer = c(n.surfaceHighest), onTertiaryContainer = c(n.onSurface),
        background = c(n.background), onBackground = c(n.onBackground),
        surface = c(n.surface), onSurface = c(n.onSurface),
        surfaceVariant = c(n.surfaceVariant), onSurfaceVariant = c(n.onSurfaceVariant),
        surfaceTint = c(a.primary),
        inverseSurface = c(n.inverseSurface), inverseOnSurface = c(n.inverseOnSurface),
        error = c(s.danger), onError = c(s.onDanger),
        errorContainer = c(s.dangerContainer), onErrorContainer = c(s.onDangerContainer),
        outline = c(n.outline), outlineVariant = c(n.outlineVariant), scrim = Color.Black,
        surfaceBright = c(n.surfaceLowest), surfaceDim = c(n.surfaceHighest),
        surfaceContainerLowest = c(n.surfaceLowest), surfaceContainerLow = c(n.surfaceLow),
        surfaceContainer = c(n.surface), surfaceContainerHigh = c(n.surfaceHigh),
        surfaceContainerHighest = c(n.surfaceHighest),
    )
}

@Composable
fun MasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AccentPalette = AccentPalette.Default,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme, accent) { buildColorScheme(darkTheme, accent) }
    val extra = remember(darkTheme) {
        toExtra(if (darkTheme) MasterPalette.darkSemantic else MasterPalette.lightSemantic)
    }
    CompositionLocalProvider(LocalExtraColors provides extra, LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MasterTypography,
            shapes = MasterShapes,
            content = content,
        )
    }
}
