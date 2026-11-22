package com.chandan.master.presentation.theme

import com.chandan.master.domain.model.AccentPalette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Design tokens as plain ARGB values (no Compose dependency) so they can be unit-tested
 * for contrast and for the "no blue / cyan / teal / purple" rule.
 *
 * 60% = background neutrals, 30% = surfaces, 10% = accent.
 */
data class MasterNeutrals(
    val background: Long,
    val surfaceLowest: Long,
    val surfaceLow: Long,
    val surface: Long,
    val surfaceHigh: Long,
    val surfaceHighest: Long,
    val surfaceVariant: Long,
    val outline: Long,
    val outlineVariant: Long,
    val onBackground: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val inverseSurface: Long,
    val inverseOnSurface: Long,
    val secondary: Long,
    val onSecondary: Long,
)

data class AccentColors(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
)

data class SemanticColors(
    val success: Long,
    val danger: Long,
    val onDanger: Long,
    val dangerContainer: Long,
    val onDangerContainer: Long,
)

object MasterPalette {
    val dark = MasterNeutrals(
        background = 0xFF121110, surfaceLowest = 0xFF0E0D0C, surfaceLow = 0xFF181716,
        surface = 0xFF1C1B19, surfaceHigh = 0xFF262421, surfaceHighest = 0xFF302D2A,
        surfaceVariant = 0xFF262421, outline = 0xFF857E70, outlineVariant = 0xFF3A3732,
        onBackground = 0xFFEDE6D6, onSurface = 0xFFEDE6D6, onSurfaceVariant = 0xFFB0A898,
        inverseSurface = 0xFFEDE6D6, inverseOnSurface = 0xFF1C1B19,
        secondary = 0xFFC9BFAA, onSecondary = 0xFF1C1B19,
    )

    val light = MasterNeutrals(
        background = 0xFFF6F1E7, surfaceLowest = 0xFFFFFCF6, surfaceLow = 0xFFF9F5EC,
        surface = 0xFFFBF8F1, surfaceHigh = 0xFFEDE6D8, surfaceHighest = 0xFFE5DDCC,
        surfaceVariant = 0xFFEDE6D8, outline = 0xFF7F7767, outlineVariant = 0xFFD6CEBD,
        onBackground = 0xFF1C1A17, onSurface = 0xFF1C1A17, onSurfaceVariant = 0xFF5A5448,
        inverseSurface = 0xFF2B2926, inverseOnSurface = 0xFFF6F1E7,
        secondary = 0xFF5E574A, onSecondary = 0xFFFFFFFF,
    )

    val darkSemantic = SemanticColors(
        success = 0xFF6DAA68, danger = 0xFFEF7A74, onDanger = 0xFF2A0B09,
        dangerContainer = 0xFF4A1F1C, onDangerContainer = 0xFFFFD9D6,
    )
    val lightSemantic = SemanticColors(
        success = 0xFF2F6B2C, danger = 0xFFB3261E, onDanger = 0xFFFFFFFF,
        dangerContainer = 0xFFF9DEDA, onDangerContainer = 0xFF410E0B,
    )

    private val darkPrimaries = mapOf(
        AccentPalette.VERMILION to 0xFFE8693AL,
        AccentPalette.BURNT_ORANGE to 0xFFD9742BL,
        AccentPalette.AMBER to 0xFFD9A441L,
        AccentPalette.CRIMSON to 0xFFE5625EL,
        AccentPalette.RUST to 0xFFCC7C57L,
    )
    private val lightPrimaries = mapOf(
        AccentPalette.VERMILION to 0xFFAE3D16L,
        AccentPalette.BURNT_ORANGE to 0xFF9E4A0DL,
        AccentPalette.AMBER to 0xFF7C5B08L,
        AccentPalette.CRIMSON to 0xFFB3282DL,
        AccentPalette.RUST to 0xFF8F4326L,
    )

    fun accent(palette: AccentPalette, dark: Boolean): AccentColors {
        return if (dark) {
            val p = darkPrimaries.getValue(palette)
            AccentColors(
                primary = p,
                onPrimary = 0xFF140C07,
                primaryContainer = ColorMath.blend(p, MasterPalette.dark.surface, 0.30),
                onPrimaryContainer = ColorMath.blend(p, 0xFFFFFFFF, 0.60),
            )
        } else {
            val p = lightPrimaries.getValue(palette)
            AccentColors(
                primary = p,
                onPrimary = 0xFFFFFFFF,
                primaryContainer = ColorMath.blend(p, MasterPalette.light.surface, 0.16),
                onPrimaryContainer = ColorMath.blend(p, 0xFF000000, 0.45),
            )
        }
    }
}

object ColorMath {
    private fun ch(c: Long, shift: Int): Int = ((c shr shift) and 0xFF).toInt()
    fun red(c: Long) = ch(c, 16)
    fun green(c: Long) = ch(c, 8)
    fun blue(c: Long) = ch(c, 0)

    /** Returns [fg] blended over [bg]; [fraction] is the share of [fg] (0..1). Result is opaque. */
    fun blend(fg: Long, bg: Long, fraction: Double): Long {
        fun mix(a: Int, b: Int) = (a * fraction + b * (1 - fraction)).roundToLong()
        return 0xFF000000L or (mix(red(fg), red(bg)) shl 16) or (mix(green(fg), green(bg)) shl 8) or mix(blue(fg), blue(bg))
    }

    private fun lin(v: Int): Double {
        val s = v / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(c: Long): Double = 0.2126 * lin(red(c)) + 0.7152 * lin(green(c)) + 0.0722 * lin(blue(c))

    /** WCAG 2.x contrast ratio, 1..21. */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a); val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Hue in degrees 0..360 and saturation 0..1 (HSV). */
    fun hueSaturation(c: Long): Pair<Double, Double> {
        val r = red(c) / 255.0; val g = green(c) / 255.0; val b = blue(c) / 255.0
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn
        val s = if (mx == 0.0) 0.0 else d / mx
        val h = when {
            d == 0.0 -> 0.0
            mx == r -> 60 * (((g - b) / d) % 6)
            mx == g -> 60 * (((b - r) / d) + 2)
            else -> 60 * (((r - g) / d) + 4)
        }
        return (if (h < 0) h + 360 else h) to s
    }
}
