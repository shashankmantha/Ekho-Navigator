package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.database.model.toDomainModel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.SharedLocation
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun `firestoreDataToEntity reads placeId — guards against the silent-drop regression`() {
        // Recipients depend on placeId being parsed off the Firestore doc; without this,
        // shared events whose owner pinned them to a marker collapse to no-link rows.
        val entity = firestoreDataToEntity(
            id = "evt-1",
            data = baseFirestoreData() + ("placeId" to "marker_42"),
        )

        assertNotNull(entity)
        assertEquals("marker_42", entity!!.placeId)
    }

    @Test
    fun `firestoreDataToEntity rebuilds the customLocation snapshot for recipients`() {
        // The whole point of the customLocation field — recipients must see lat/lng/title
        // even though they don't own the source marker.
        val entity = firestoreDataToEntity(
            id = "evt-1",
            data = baseFirestoreData() + (
                "customLocation" to mapOf(
                    "title" to "Coffee spot",
                    "latitude" to 34.16,
                    "longitude" to -119.04,
                )
                ),
        )

        assertNotNull(entity)
        assertEquals("Coffee spot", entity!!.customLocationTitle)
        assertEquals(34.16, entity.customLocationLatitude)
        assertEquals(-119.04, entity.customLocationLongitude)
    }

    @Test
    fun `firestoreDataToEntity domain round-trip preserves customLocation`() {
        // End-to-end check: entity → domain reconstructs SharedLocation from the flat columns.
        val entity = firestoreDataToEntity(
            id = "evt-1",
            data = baseFirestoreData() + (
                "customLocation" to mapOf(
                    "title" to "Coffee spot",
                    "latitude" to 34.16,
                    "longitude" to -119.04,
                )
                ),
        )!!
        val domain = entity.toDomainModel()

        assertEquals(SharedLocation("Coffee spot", 34.16, -119.04), domain.customLocation)
    }

    @Test
    fun `firestoreDataToEntity reads ownerDisplayName — drives 'Invited by' rendering`() {
        val entity = firestoreDataToEntity(
            id = "evt-1",
            data = baseFirestoreData() + ("ownerDisplayName" to "Alice"),
        )

        assertEquals("Alice", entity?.ownerDisplayName)
    }

    @Test
    fun `firestoreDataToEntity tags entity with the supplied source`() {
        // Owner's second device receives their own event back through the listener and
        // needs USER_CREATED, not SHARED — the override has to actually take effect.
        val asShared = firestoreDataToEntity(id = "evt-1", data = baseFirestoreData())
        val asOwned = firestoreDataToEntity(id = "evt-1", data = baseFirestoreData(), source = EventSource.USER_CREATED)

        assertEquals(EventSource.SHARED, asShared?.source)
        assertEquals(EventSource.USER_CREATED, asOwned?.source)
    }

    @Test
    fun `firestoreDataToEntity reads type and round-trips it through toDomainModel`() {
        // Silent-drop guard for the EventType field: a sender's type=ASSIGNMENT must
        // survive the wire → entity → domain hops on every recipient device.
        val entity = firestoreDataToEntity(
            id = "evt-1",
            data = baseFirestoreData() + ("type" to "ASSIGNMENT"),
        )!!

        assertEquals(EventType.ASSIGNMENT, entity.type)
        assertEquals(EventType.ASSIGNMENT, entity.toDomainModel().type)
    }

    @Test
    fun `firestoreDataToEntity defaults missing or unknown type to EVENT`() {
        // Back-compat: pre-EventType Firestore docs have no "type" key. Forward-compat:
        // a future enum value not yet known to this client must not crash the parser.
        val noType = firestoreDataToEntity(id = "evt-1", data = baseFirestoreData())
        val unknownType = firestoreDataToEntity(
            id = "evt-1",
            data = baseFirestoreData() + ("type" to "UNFAMILIAR_VALUE"),
        )

        assertEquals(EventType.EVENT, noType?.type)
        assertEquals(EventType.EVENT, unknownType?.type)
    }

    @Test
    fun `toCustomEventEntity carries type through to the entity`() {
        // User-create paths must propagate type so a future user-typed assignment ships
        // intact to Room (and from there to Firestore via the push paths).
        val domain = sampleDomainEvent().copy(type = EventType.ASSIGNMENT)

        val entity = domain.toCustomEventEntity()

        assertEquals(EventType.ASSIGNMENT, entity.type)
    }

    @Test
    fun `firestoreDataToEntity returns null when required fields are missing`() {
        // Missing title means the doc was malformed (or partially deleted) — refuse the row
        // rather than insert a corrupt entity Room would later choke on.
        val noTitle = firestoreDataToEntity(id = "evt-1", data = baseFirestoreData() - "title")
        val noStart = firestoreDataToEntity(id = "evt-1", data = baseFirestoreData() - "startTime")
        val nullData = firestoreDataToEntity(id = "evt-1", data = null)

        assertNull(noTitle)
        assertNull(noStart)
        assertNull(nullData)
    }

    private fun baseFirestoreData(): Map<String, Any?> = mapOf(
        "title" to "Pizza Night",
        "description" to "BYO drinks",
        "location" to "Bell Tower",
        "startTime" to 1782000000000L,
        "endTime" to 1782007200000L,
        "categories" to listOf("GENERAL"),
        "ownerUid" to "owner-1",
    )

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
