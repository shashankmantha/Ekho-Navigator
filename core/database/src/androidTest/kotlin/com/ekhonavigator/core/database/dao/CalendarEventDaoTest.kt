package com.ekhonavigator.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ekhonavigator.core.database.EkhoDatabase
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.model.EventCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Instrumented tests for [CalendarEventDao].
 *
 * ## Why `src/androidTest/` (not `src/test/`)?
 *
 * Room is an Android library — its database engine (SQLite) runs on the
 * Android runtime. JVM tests can't create a Room database because there's
 * no Android SQLite driver on the JVM. So DAO tests must run on an Android
 * device or emulator.
 *
 * ## How in-memory Room works
 *
 * [Room.inMemoryDatabaseBuilder] creates a database that lives entirely
 * in RAM — no files on disk. It's created fresh for each test (via @Before)
 * and destroyed after (via @After), so tests are completely isolated.
 * `allowMainThreadQueries()` lets us call DAO methods synchronously in
 * tests without needing to worry about threading.
 *
 * ## What these tests verify
 *
 * - CRUD operations (upsert, query, update, delete)
 * - Flow emissions (Room's Flow re-emits when data changes)
 * - Date range filtering (the WHERE clause in the SQL query)
 * - Bookmark toggle logic
 * - Stale event cleanup
 */
@RunWith(AndroidJUnit4::class)
class CalendarEventDaoTest {

    private lateinit var database: EkhoDatabase
    private lateinit var dao: CalendarEventDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EkhoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.calendarEventDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ---- Helper to create test entities ----

    private fun testEntity(
        uid: String = "test-uid",
        title: String = "Test Event",
        startTime: Instant = Instant.parse("2026-03-15T10:00:00Z"),
        endTime: Instant = Instant.parse("2026-03-15T12:00:00Z"),
        categories: List<EventCategory> = listOf(EventCategory.GENERAL),
        isBookmarked: Boolean = false,
    ) = CalendarEventEntity(
        uid = uid,
        title = title,
        description = "Description",
        location = "Location",
        startTime = startTime,
        endTime = endTime,
        categories = categories,
        url = "",
        status = "CONFIRMED",
        isBookmarked = isBookmarked,
        lastSyncedAt = Instant.now(),
    )

    // ---- Tests ----

    @Test
    fun upsertAndObserveAllEvents() = runTest {
        val event1 = testEntity(uid = "1", title = "First")
        val event2 = testEntity(uid = "2", title = "Second")

        dao.upsertEvents(listOf(event1, event2))

        val events = dao.observeAllEvents().first()
        assertEquals(2, events.size)
    }

    @Test
    fun upsertUpdatesExistingEvent() = runTest {
        val original = testEntity(uid = "1", title = "Original")
        dao.upsertEvents(listOf(original))

        // Upsert with same UID but different title
        val updated = original.copy(title = "Updated")
        dao.upsertEvents(listOf(updated))

        val events = dao.observeAllEvents().first()
        assertEquals(1, events.size)
        assertEquals("Updated", events[0].title)
    }

    @Test
    fun observeEventById() = runTest {
        val event = testEntity(uid = "find-me", title = "Target Event")
        dao.upsertEvents(listOf(event))

        val found = dao.observeEventById("find-me").first()
        assertNotNull(found)
        assertEquals("Target Event", found.title)
    }

    @Test
    fun observeEventByIdReturnsNullForMissingId() = runTest {
        val result = dao.observeEventById("nonexistent").first()
        assertNull(result)
    }

    @Test
    fun observeBookmarkedEvents() = runTest {
        val bookmarked = testEntity(uid = "b1", isBookmarked = true)
        val normal = testEntity(uid = "n1", isBookmarked = false)

        dao.upsertEvents(listOf(bookmarked, normal))

        val result = dao.observeBookmarkedEvents().first()
        assertEquals(1, result.size)
        assertEquals("b1", result[0].uid)
    }

    @Test
    fun updateBookmark() = runTest {
        val event = testEntity(uid = "1", isBookmarked = false)
        dao.upsertEvents(listOf(event))

        // Toggle bookmark on
        dao.updateBookmark("1", true)

        val updated = dao.getEventById("1")
        assertNotNull(updated)
        assertTrue(updated.isBookmarked)
    }

    @Test
    fun observeEventsByDateRange() = runTest {
        val march15 = testEntity(
            uid = "march",
            startTime = Instant.parse("2026-03-15T10:00:00Z"),
        )
        val april5 = testEntity(
            uid = "april",
            startTime = Instant.parse("2026-04-05T10:00:00Z"),
        )

        dao.upsertEvents(listOf(march15, april5))

        // Query March only: [March 1, April 1)
        val marchStart = Instant.parse("2026-03-01T00:00:00Z")
        val marchEnd = Instant.parse("2026-04-01T00:00:00Z")

        val marchEvents = dao.observeEventsByDateRange(marchStart, marchEnd).first()
        assertEquals(1, marchEvents.size)
        assertEquals("march", marchEvents[0].uid)
    }

    @Test
    fun eventsAreOrderedByStartTime() = runTest {
        val later = testEntity(
            uid = "later",
            startTime = Instant.parse("2026-03-20T10:00:00Z"),
        )
        val earlier = testEntity(
            uid = "earlier",
            startTime = Instant.parse("2026-03-10T10:00:00Z"),
        )

        // Insert in reverse order
        dao.upsertEvents(listOf(later, earlier))

        val events = dao.observeAllEvents().first()
        assertEquals("earlier", events[0].uid)
        assertEquals("later", events[1].uid)
    }

    @Test
    fun deleteEventsNotIn() = runTest {
        dao.upsertEvents(
            listOf(
                testEntity(uid = "keep-1"),
                testEntity(uid = "keep-2"),
                testEntity(uid = "remove"),
            )
        )

        // Delete events NOT in the active list
        dao.deleteEventsNotIn(listOf("keep-1", "keep-2"))

        val remaining = dao.observeAllEvents().first()
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.uid.startsWith("keep") })
    }

    @Test
    fun deleteOldEvents() = runTest {
        val old = testEntity(
            uid = "old",
            endTime = Instant.parse("2020-01-01T00:00:00Z"),
            isBookmarked = false,
        )
        val oldBookmarked = testEntity(
            uid = "old-bookmarked",
            endTime = Instant.parse("2020-01-01T00:00:00Z"),
            isBookmarked = true,
        )
        val recent = testEntity(
            uid = "recent",
            endTime = Instant.parse("2026-06-01T00:00:00Z"),
        )

        dao.upsertEvents(listOf(old, oldBookmarked, recent))

        // Delete non-bookmarked events older than 2025
        dao.deleteOldEvents(Instant.parse("2025-01-01T00:00:00Z"))

        val remaining = dao.observeAllEvents().first()
        assertEquals(2, remaining.size)
        // The old bookmarked event should be preserved
        assertTrue(remaining.any { it.uid == "old-bookmarked" })
        assertTrue(remaining.any { it.uid == "recent" })
    }

    @Test
    fun categoryConverterRoundTrips() = runTest {
        val event = testEntity(
            uid = "cats",
            categories = listOf(EventCategory.ALUMNI, EventCategory.STAFF, EventCategory.COMMUNITY),
        )
        dao.upsertEvents(listOf(event))

        val loaded = dao.getEventById("cats")
        assertNotNull(loaded)
        assertEquals(
            listOf(EventCategory.ALUMNI, EventCategory.STAFF, EventCategory.COMMUNITY),
            loaded.categories,
        )
    }
}
