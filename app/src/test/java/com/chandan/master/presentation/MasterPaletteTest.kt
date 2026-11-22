package com.chandan.master.presentation

import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.presentation.theme.ColorMath
import com.chandan.master.presentation.theme.MasterNeutrals
import com.chandan.master.presentation.theme.MasterPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterPaletteTest {
    private fun allColors(): Map<String, Long> {
        val out = linkedMapOf<String, Long>()
        fun neutrals(prefix: String, n: MasterNeutrals) {
            out["$prefix.background"] = n.background; out["$prefix.surface"] = n.surface
            out["$prefix.surfaceHigh"] = n.surfaceHigh; out["$prefix.surfaceHighest"] = n.surfaceHighest
            out["$prefix.outline"] = n.outline; out["$prefix.onSurface"] = n.onSurface
            out["$prefix.onSurfaceVariant"] = n.onSurfaceVariant; out["$prefix.secondary"] = n.secondary
        }
        neutrals("dark", MasterPalette.dark); neutrals("light", MasterPalette.light)
        for (dark in listOf(true, false)) for (p in AccentPalette.entries) {
            val a = MasterPalette.accent(p, dark); val k = "${if (dark) "dark" else "light"}.$p"
            out["$k.primary"] = a.primary; out["$k.container"] = a.primaryContainer; out["$k.onContainer"] = a.onPrimaryContainer
        }
        val sems = mapOf("dark" to MasterPalette.darkSemantic, "light" to MasterPalette.lightSemantic)
        sems.forEach { (n, s) -> out["$n.success"] = s.success; out["$n.danger"] = s.danger }
        return out
    }

    /** No blue, navy, cyan, teal or blue-purple: any coloured (saturated) token must avoid hues 165°–295°. */
    @Test fun noBlueCyanTealOrPurpleHues() {
        for ((name, c) in allColors()) {
            val (h, s) = ColorMath.hueSaturation(c)
            val forbidden = s > 0.10 && h in 165.0..295.0
            assertTrue("$name = %08X has forbidden hue %.0f (sat %.2f)".format(c, h, s), !forbidden)
        }
    }

    @Test fun backgroundTextContrastMeetsAAA() {
        for (n in listOf(MasterPalette.dark, MasterPalette.light)) {
            assertTrue(ColorMath.contrast(n.onBackground, n.background) >= 7.0)
            assertTrue(ColorMath.contrast(n.onSurface, n.surface) >= 7.0)
            assertTrue(ColorMath.contrast(n.onSurface, n.surfaceHighest) >= 7.0)
        }
    }

    @Test fun secondaryTextContrastMeetsAA() {
        for (n in listOf(MasterPalette.dark, MasterPalette.light)) {
            assertTrue(ColorMath.contrast(n.onSurfaceVariant, n.background) >= 4.5)
            assertTrue(ColorMath.contrast(n.onSurfaceVariant, n.surface) >= 4.5)
            assertTrue(ColorMath.contrast(n.onSurfaceVariant, n.surfaceHigh) >= 4.5)
            assertTrue(ColorMath.contrast(n.onSecondary, n.secondary) >= 4.5)
        }
    }

    @Test fun inputBorderContrastMeetsNonTextRequirement() {
        for (n in listOf(MasterPalette.dark, MasterPalette.light)) {
            assertTrue(ColorMath.contrast(n.outline, n.background) >= 3.0)
            assertTrue(ColorMath.contrast(n.outline, n.surface) >= 3.0)
        }
    }

    @Test fun everyAccentIsReadableInBothThemes() {
        for (dark in listOf(true, false)) {
            val n = if (dark) MasterPalette.dark else MasterPalette.light
            for (p in AccentPalette.entries) {
                val a = MasterPalette.accent(p, dark)
                val tag = "$p dark=$dark"
                assertTrue("$tag onPrimary/primary", ColorMath.contrast(a.onPrimary, a.primary) >= 4.5)
                assertTrue("$tag onContainer/container", ColorMath.contrast(a.onPrimaryContainer, a.primaryContainer) >= 4.5)
                assertTrue("$tag primary vs background (non-text)", ColorMath.contrast(a.primary, n.background) >= 3.0)
                assertTrue("$tag primary vs surface (non-text)", ColorMath.contrast(a.primary, n.surface) >= 3.0)
            }
        }
    }

    /** The accent is used as link/text colour (e.g. Developer & Social Links), so it needs 4.5:1 on backgrounds/surfaces. */
    @Test fun everyAccentIsUsableAsTextOnBackgroundsAndSurfaces() {
        for (dark in listOf(true, false)) {
            val n = if (dark) MasterPalette.dark else MasterPalette.light
            for (p in AccentPalette.entries) {
                val a = MasterPalette.accent(p, dark)
                for ((label, bg) in listOf("background" to n.background, "surface" to n.surface, "surfaceHigh" to n.surfaceHigh)) {
                    val ratio = ColorMath.contrast(a.primary, bg)
                    assertTrue("$p dark=$dark primary on $label = %.2f".format(ratio), ratio >= 4.5)
                }
            }
        }
    }

    @Test fun semanticColorsAreReadable() {
        for ((s, n) in listOf(MasterPalette.darkSemantic to MasterPalette.dark, MasterPalette.lightSemantic to MasterPalette.light)) {
            assertTrue(ColorMath.contrast(s.success, n.background) >= 3.0)
            assertTrue(ColorMath.contrast(s.danger, n.background) >= 3.0)
            assertTrue(ColorMath.contrast(s.onDanger, s.danger) >= 4.5)
            assertTrue(ColorMath.contrast(s.onDangerContainer, s.dangerContainer) >= 4.5)
        }
    }

    @Test fun sixtyThirtyTenSurfaceOrdering() {
        // Dark theme gets lighter as elevation rises; light theme gets darker.
        val d = MasterPalette.dark
        assertTrue(ColorMath.luminance(d.surfaceLowest) < ColorMath.luminance(d.background))
        assertTrue(ColorMath.luminance(d.background) < ColorMath.luminance(d.surface))
        assertTrue(ColorMath.luminance(d.surface) < ColorMath.luminance(d.surfaceHigh))
        assertTrue(ColorMath.luminance(d.surfaceHigh) < ColorMath.luminance(d.surfaceHighest))
        val l = MasterPalette.light
        assertTrue(ColorMath.luminance(l.surfaceHigh) < ColorMath.luminance(l.surface))
        assertTrue(ColorMath.luminance(l.surfaceHighest) < ColorMath.luminance(l.surfaceHigh))
    }

    @Test fun colorMathSanity() {
        assertEquals(21.0, ColorMath.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, ColorMath.contrast(0xFF123456, 0xFF123456), 0.0001)
        val mid = ColorMath.blend(0xFFFFFFFF, 0xFF000000, 0.5)
        assertEquals(128, ColorMath.red(mid)); assertEquals(128, ColorMath.green(mid)); assertEquals(128, ColorMath.blue(mid))
        assertEquals(0.0, ColorMath.hueSaturation(0xFFFF0000).first, 0.01)
        assertEquals(120.0, ColorMath.hueSaturation(0xFF00FF00).first, 0.01)
        assertEquals(240.0, ColorMath.hueSaturation(0xFF0000FF).first, 0.01)
    }
}
