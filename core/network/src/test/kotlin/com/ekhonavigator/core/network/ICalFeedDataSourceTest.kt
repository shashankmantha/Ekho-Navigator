package com.ekhonavigator.core.network

import com.ekhonavigator.core.model.EventCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the iCal parser.
 *
 * ## Why this matters
 *
 * The parser is the seam between the 25Live Publisher feed and our app.
 * Every bug here silently corrupts data for every user — wrong category,
 * missing Trumba fields, dropped events. Because the feed format is
 * quirky (Trumba stuffs real categories into X-TRUMBA-CUSTOMFIELD,
 * multi-values are comma-escaped, HTML entities leak into plain-text
 * fields), assumptions break quietly.
 *
 * ## Why top-level functions
 *
 * [parseICalFeed] and [decodeHtmlEntities] are top-level internal
 * functions so tests never have to instantiate [ICalFeedDataSource] —
 * its [okhttp3.OkHttpClient] dependency triggers `android.util.Log` at
 * class init, which crashes in a pure JVM unit-test environment.
 */
class ICalFeedDataSourceTest {

    @Test
    fun `parses basic VEVENT fields`() {
        val events = parseICalFeed(buildFeed(buildEvent()))

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("evt-1", event.uid)
        assertEquals("Career Fair", event.summary)
        assertEquals("All majors welcome", event.description)
        assertEquals("Broome Library", event.location)
        assertEquals("CONFIRMED", event.status)
        assertEquals("https://example.edu/events/1", event.url)
    }

    @Test
    fun `parses all Trumba custom fields`() {
        // Regression guard for the Trumba field rework — Event Name,
        // Organization, Event Type must all come through.
        val event = parseICalFeed(buildFeed(buildEvent())).first()

        assertEquals("Spring Career Fair 2026", event.eventName)
        assertEquals("Career Development", event.organization)
        assertEquals("Networking", event.eventType)
    }

    @Test
    fun `parses Trumba Categories enum into EventCategory list`() {
        // The standard CATEGORIES property is useless garbage ("CSUCI Events
        // Calendar 25Live"). Real categories live in X-TRUMBA-CUSTOMFIELD
        // with NAME="Categories".
        val event = parseICalFeed(buildFeed(buildEvent())).first()

        assertEquals(listOf(EventCategory.STAFF), event.categories)
    }

    @Test
    fun `splits multi-value Categories on commas`() {
        val event = parseICalFeed(
            buildFeed(buildEvent(categories = "Alumni, Community, Staff")),
        ).first()

        assertTrue(
            event.categories.containsAll(
                listOf(EventCategory.ALUMNI, EventCategory.COMMUNITY, EventCategory.STAFF),
            ),
        )
    }

    @Test
    fun `falls back to GENERAL when Categories field is missing`() {
        val event = parseICalFeed(buildFeed(buildEvent(categories = null))).first()

        assertEquals(listOf(EventCategory.GENERAL), event.categories)
    }

    @Test
    fun `defaults status to CONFIRMED when STATUS is absent`() {
        val event = parseICalFeed(buildFeed(buildEvent(status = null))).first()

        assertEquals("CONFIRMED", event.status)
    }

    @Test
    fun `uses DTSTART as DTEND when DTEND is missing`() {
        // Some all-day events omit DTEND; the parser must not crash and
        // should default endTime to startTime so the domain layer has a
        // valid interval.
        val event = parseICalFeed(buildFeed(buildEvent(dtEnd = null))).first()

        assertEquals(event.dtStart, event.dtEnd)
    }

    @Test
    fun `skips malformed events instead of failing whole feed`() {
        // One missing DTSTART (skipped by parseEvent) + one valid (kept).
        // A single bad event must not blank out the entire sync for the user.
        // We pick DTSTART rather than UID because iCal4j's builder may reject
        // a UID-less VEVENT up-front, preventing our per-event catch from running.
        val malformed = buildEvent(uid = "bad-evt", dtStart = null)
        val valid = buildEvent()

        val events = parseICalFeed(buildFeed(malformed + valid))

        assertEquals(1, events.size)
        assertEquals("evt-1", events.first().uid)
    }

    @Test
    fun `returns empty list on completely malformed payload`() {
        // Garbage input must return [] rather than throwing — a 500 error
        // page served where an .ics was expected shouldn't crash sync.
        val events = parseICalFeed("not an ical feed at all")

        assertNotNull(events)
        assertTrue(events.isEmpty())
    }

    // region HTML entity decoder

    @Test
    fun `decodes numeric HTML entities used by Trumba`() {
        // &#39; is an apostrophe — appears constantly in event titles like
        // "President&#39;s Welcome". Leaving it raw makes titles look broken.
        assertEquals("President's Welcome", decodeHtmlEntities("President&#39;s Welcome"))
        assertEquals("2 < 3", decodeHtmlEntities("2 &#60; 3"))
    }

    @Test
    fun `decodes named HTML entities`() {
        assertEquals("A & B", decodeHtmlEntities("A &amp; B"))
        assertEquals("\"quoted\"", decodeHtmlEntities("&quot;quoted&quot;"))
        assertEquals("<tag>", decodeHtmlEntities("&lt;tag&gt;"))
    }

    @Test
    fun `decodes hex HTML entities`() {
        // Smart quote via hex: &#x2019; → '
        assertEquals("it\u2019s", decodeHtmlEntities("it&#x2019;s"))
    }

    @Test
    fun `leaves unknown entities intact rather than dropping them`() {
        // If Trumba ever emits an entity we don't know, we'd rather keep
        // it visible in the UI than silently strip characters.
        assertEquals("&unknown;", decodeHtmlEntities("&unknown;"))
    }

    @Test
    fun `trims decoded output`() {
        assertEquals("hello", decodeHtmlEntities("  hello  "))
    }

    // endregion

    // ---- test helpers ----

    /**
     * Builds a single VEVENT block. `null` arguments omit that line entirely.
     * iCal requires CRLF line endings (RFC 5545), and iCal4j enforces that.
     */
    private fun buildEvent(
        uid: String? = "evt-1",
        summary: String? = "Career Fair",
        dtStart: String? = "20260501T170000Z",
        dtEnd: String? = "20260501T200000Z",
        location: String? = "Broome Library",
        description: String? = "All majors welcome",
        status: String? = "CONFIRMED",
        url: String? = "https://example.edu/events/1",
        eventName: String? = "Spring Career Fair 2026",
        organization: String? = "Career Development",
        eventType: String? = "Networking",
        categories: String? = "Staff",
    ): String = buildString {
        append("BEGIN:VEVENT\r\n")
        if (uid != null) append("UID:$uid\r\n")
        if (summary != null) append("SUMMARY:$summary\r\n")
        if (dtStart != null) append("DTSTART:$dtStart\r\n")
        if (dtEnd != null) append("DTEND:$dtEnd\r\n")
        if (location != null) append("LOCATION:$location\r\n")
        if (description != null) append("DESCRIPTION:$description\r\n")
        if (status != null) append("STATUS:$status\r\n")
        if (url != null) append("URL:$url\r\n")
        if (eventName != null) {
            append("X-TRUMBA-CUSTOMFIELD;NAME=\"Event Name\";ID=1;TYPE=Text:$eventName\r\n")
        }
        if (organization != null) {
            append("X-TRUMBA-CUSTOMFIELD;NAME=\"Organization\";ID=2;TYPE=Text:$organization\r\n")
        }
        if (eventType != null) {
            append("X-TRUMBA-CUSTOMFIELD;NAME=\"Event Type\";ID=3;TYPE=Text:$eventType\r\n")
        }
        if (categories != null) {
            append("X-TRUMBA-CUSTOMFIELD;NAME=\"Categories\";ID=23227;TYPE=Enumeration:$categories\r\n")
        }
        append("END:VEVENT\r\n")
    }

    private fun buildFeed(eventBlocks: String): String =
        "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Test//Test//EN\r\n" +
            eventBlocks +
            "END:VCALENDAR\r\n"
}
