package com.ekhonavigator.core.canvas.network

import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkHeaderTest {

    @Test
    fun `extracts the next URL from a typical multi-rel Canvas Link header`() {
        val headers = headersOf(
            "Link",
            """<https://csuci.instructure.com/api/v1/planner/items?page=bookmark%3AABC>; rel="next",""" +
                """<https://csuci.instructure.com/api/v1/planner/items?page=first>; rel="first",""" +
                """<https://csuci.instructure.com/api/v1/planner/items?page=current>; rel="current"""",
        )

        assertEquals(
            "https://csuci.instructure.com/api/v1/planner/items?page=bookmark%3AABC",
            nextPageUrl(headers),
        )
    }

    @Test
    fun `returns null when the Link header is absent — last page reached`() {
        assertNull(nextPageUrl(headersOf()))
    }

    @Test
    fun `returns null when no rel=next entry exists — only first or last available`() {
        val headers = headersOf(
            "Link",
            """<https://example.test/api/v1/planner/items?page=first>; rel="first",""" +
                """<https://example.test/api/v1/planner/items?page=current>; rel="current"""",
        )

        assertNull(nextPageUrl(headers))
    }

    @Test
    fun `tolerates whitespace and unquoted rel values`() {
        // RFC 5988 allows rel without quotes; some servers do this. Don't be brittle.
        val headers = headersOf("Link", " <https://example.test/page2> ; rel=next ")

        assertEquals("https://example.test/page2", nextPageUrl(headers))
    }

    @Test
    fun `header lookup is case-insensitive — Canvas sometimes lowercases the header name`() {
        val headers = headersOf("link", """<https://example.test/page2>; rel="next"""")

        assertEquals("https://example.test/page2", nextPageUrl(headers))
    }
}
