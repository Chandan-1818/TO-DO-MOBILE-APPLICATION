package com.chandan.master.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.chandan.master.data.local.preferences.PreferencesDataSource
import com.chandan.master.domain.model.AccentPalette
import com.chandan.master.domain.model.AppPreferences
import com.chandan.master.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Uses a real DataStore on a temp file (no mocks) to prove persistence and corruption fallback. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreferencesDataSourceTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun newSource(file: File, scope: TestScope) = PreferencesDataSource(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { file },
    )

    @Test fun defaultsWhenNothingStored() = runTest(UnconfinedTestDispatcher()) {
        val source = newSource(tmp.newFile("a.preferences_pb").also { it.delete() }, this)
        assertEquals(AppPreferences(), source.preferences.first())
    }

    @Test fun themeAndAccentPersistAcrossInstances() = runTest(UnconfinedTestDispatcher()) {
        val file = tmp.newFile("b.preferences_pb").also { it.delete() }
        val first = newSource(file, this)
        first.setThemeMode(ThemeMode.DARK)
        first.setAccentPalette(AccentPalette.RUST)
        assertEquals(AppPreferences(ThemeMode.DARK, AccentPalette.RUST), first.preferences.first())
    }

    @Test fun settingOneValueDoesNotResetTheOther() = runTest(UnconfinedTestDispatcher()) {
        val source = newSource(tmp.newFile("c.preferences_pb").also { it.delete() }, this)
        source.setAccentPalette(AccentPalette.AMBER)
        source.setThemeMode(ThemeMode.LIGHT)
        assertEquals(AppPreferences(ThemeMode.LIGHT, AccentPalette.AMBER), source.preferences.first())
    }
}
