package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Mapping functions sit between three worlds: the iCal wire format
 * ([NetworkCalendarEvent]), the Room DB ([CalendarEventEntity]), and
 * user-authored events ([CalendarEvent] → entity for Firestore sync).
 *
 * A silent mis-mapping here means a user loses a bookmark, an event
 * shows up in the wrong tab, or a Trumba field (Organization, Event Name)
 * vanishes on sync. These are exactly the regressions you don't notice
 * in manual QA, so this file covers the contracts.
 */
class MappersTest {

    private val wireEvent = NetworkCalendarEvent(
        uid = "evt-123",
        summary = "Career Fair",
        eventName = "Spring Career Fair 2026",
        description = "All majors welcome",
        location = "Broome Library",
        organization = "Career Development",
        eventType = "Networking",
        dtStart = Instant.parse("2026-05-01T17:00:00Z"),
        dtEnd = Instant.parse("2026-05-01T20:00:00Z"),
        categories = listOf(EventCategory.GENERAL),
        url = "https://example.edu/events/123",
        status = "CONFIRMED",
    )

    @Test
    fun `toEntity preserves existing bookmark across resync`() {
        // Regression guard: if a user bookmarks an event and the iCal feed
        // is refetched, the bookmark must survive. Losing it on every sync
        // would silently wipe the user's saved events.
        val entity = wireEvent.toEntity(existingBookmark = true)

        assertTrue(entity.isBookmarked)
    }

    @Test
    fun `toEntity defaults to not bookmarked for brand new events`() {
        val entity = wireEvent.toEntity()

        assertFalse(entity.isBookmarked)
    }

    @Test
    fun `toEntity passes through all Trumba custom fields`() {
        // The whole point of the Trumba rework — these fields must not
        // be dropped between the network layer and the DB.
        val entity = wireEvent.toEntity()

        assertEquals("Spring Career Fair 2026", entity.eventName)
        assertEquals("Career Development", entity.organization)
        assertEquals("Networking", entity.eventType)
    }

    @Test
    fun `toEntity attaches placeId when provided by the matcher`() {
        val entity = wireEvent.toEntity(placeId = "broome-library")

        assertEquals("broome-library", entity.placeId)
    }

    @Test
    fun `toEntity leaves placeId null when no match was found`() {
        val entity = wireEvent.toEntity()

        assertNull(entity.placeId)
    }

    @Test
    fun `toEntity maps wire fields to entity columns`() {
        val entity = wireEvent.toEntity()

        assertEquals("evt-123", entity.uid)
        assertEquals("Career Fair", entity.title) // summary → title
        assertEquals("All majors welcome", entity.description)
        assertEquals("Broome Library", entity.location)
        assertEquals(wireEvent.dtStart, entity.startTime)
        assertEquals(wireEvent.dtEnd, entity.endTime)
        assertEquals(listOf(EventCategory.GENERAL), entity.categories)
        assertEquals("CONFIRMED", entity.status)
    }

    @Test
    fun `toCustomEventEntity always marks user-owned events as bookmarked`() {
        // User-created events are inherently "saved" — the user authored them.
        // If this ever defaulted to false, custom events would vanish from
        // the user's bookmarked list immediately after creation.
        val domain = sampleDomainEvent()

        val entity = domain.toCustomEventEntity()

        assertTrue(entity.isBookmarked)
    }

    @Test
    fun `toCustomEventEntity flags USER_CREATED source and pending sync`() {
        val entity = sampleDomainEvent().toCustomEventEntity()

        assertEquals(EventSource.USER_CREATED, entity.source)
        assertTrue(entity.pendingSync) // newly created → needs Firestore upload
    }

    @Test
    fun `toCustomEventEntity preserves Trumba fields and placeId`() {
        val entity = sampleDomainEvent().toCustomEventEntity()

        assertEquals("My Custom Event", entity.eventName)
        assertEquals("Student Union", entity.organization)
        assertEquals("Meeting", entity.eventType)
        assertEquals("student-union", entity.placeId)
    }

    @Test
    fun `toCustomEventEntity uses the provided event ID override`() {
        // Custom events generate a Firestore document ID separate from the
        // domain model's id field. The override must win.
        val entity = sampleDomainEvent().toCustomEventEntity(eventId = "firestore-doc-xyz")

        assertEquals("firestore-doc-xyz", entity.uid)
    }

    private fun sampleDomainEvent() = CalendarEvent(
        id = "local-id",
        title = "Club Meeting",
        description = "Weekly sync",
        location = "Bell Tower 2565",
        startTime = Instant.parse("2026-05-02T18:00:00Z"),
        endTime = Instant.parse("2026-05-02T19:00:00Z"),
        categories = listOf(EventCategory.GENERAL),
        url = "",
        status = "CONFIRMED",
        isBookmarked = true,
        lastSyncedAt = Instant.parse("2026-05-01T00:00:00Z"),
        ownerUid = "user-42",
        eventName = "My Custom Event",
        organization = "Student Union",
        eventType = "Meeting",
        placeId = "student-union",
    )
}
