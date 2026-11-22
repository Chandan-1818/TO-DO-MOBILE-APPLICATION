package com.chandan.master.presentation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chandan.master.core.constants.ExternalLinks
import com.chandan.master.presentation.profile.DeveloperLinksSection
import com.chandan.master.presentation.theme.MasterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DeveloperLinksSectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsBothLinksAndReportsTheCorrectUrlOnClick() {
        val opened = mutableListOf<String>()
        compose.setContent { MasterTheme { DeveloperLinksSection(onOpenLink = { opened += it }) } }

        compose.onNodeWithText("Developer & Social Links").assertExists()
        compose.onNodeWithText("github.com/Chandan-1818").assertHasClickAction().performClick()
        compose.onNodeWithText("linkedin.com/in/chandans269").assertHasClickAction().performClick()

        assertEquals(listOf(ExternalLinks.GITHUB_URL, ExternalLinks.LINKEDIN_URL), opened)
    }
}
