package com.chandan.master.domain

import com.chandan.master.core.constants.ExternalLinks
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalLinksTest {
    @Test fun githubLink() {
        val uri = URI(ExternalLinks.GITHUB_URL)
        assertEquals("https", uri.scheme)
        assertEquals("github.com", uri.host)
        assertEquals("/Chandan-1818", uri.path)
    }

    @Test fun linkedInLink() {
        val uri = URI(ExternalLinks.LINKEDIN_URL)
        assertEquals("https", uri.scheme)
        assertEquals("linkedin.com", uri.host)
        assertEquals("/in/chandans269", uri.path)
    }
}
