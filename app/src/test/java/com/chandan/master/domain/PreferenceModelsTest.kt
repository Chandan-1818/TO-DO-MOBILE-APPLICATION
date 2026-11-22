package com.chandan.master.domain

import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceModelsTest {
    @Test fun themeModeRoundTrips() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorageValue(it.storageValue)) }
    }

    @Test fun themeModeFallsBackToSystemForUnknownOrMissing() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageValue("sepia"))
    }

    @Test fun accentRoundTripsAndFallsBack() {
        AccentPalette.entries.forEach { assertEquals(it, AccentPalette.fromStorageValue(it.storageValue)) }
        assertEquals(AccentPalette.VERMILION, AccentPalette.fromStorageValue("blue"))
        assertEquals(AccentPalette.VERMILION, AccentPalette.fromStorageValue(null))
    }

    @Test fun storageValuesAreUnique() {
        assertEquals(ThemeMode.entries.size, ThemeMode.entries.map { it.storageValue }.toSet().size)
        assertEquals(AccentPalette.entries.size, AccentPalette.entries.map { it.storageValue }.toSet().size)
    }

    @Test fun defaultPreferences() {
        val d = AppPreferences()
        assertEquals(ThemeMode.SYSTEM, d.themeMode)
        assertEquals(AccentPalette.VERMILION, d.accentPalette)
        assertTrue(AccentPalette.entries.none { it.storageValue.contains("blue") })
    }
}
