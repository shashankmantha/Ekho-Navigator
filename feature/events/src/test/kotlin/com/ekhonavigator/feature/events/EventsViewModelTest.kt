package com.ekhonavigator.feature.events

import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.testCalendarEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [EventsViewModel].
 *
 * ## What's being tested
 *
 * The ViewModel combines three reactive streams (events from the repo,
 * search query, and selected category) into one filtered list. These
 * tests verify that each filter works correctly, both individually and
 * combined. They also verify the past-event exclusion logic.
 *
 * ## How the test infrastructure works
 *
 * - [MainDispatcherRule]: Replaces `Dispatchers.Main` with a test
 *   dispatcher so `viewModelScope` works on the JVM (no Android needed).
 *
 * - [TestCalendarRepository]: A fake that lets us push data via `emit()`.
 *   When we emit a list of events, the ViewModel's `combine` operator
 *   receives them and re-runs its filter logic.
 *
 * - [runTest]: A coroutines-test function that runs suspending code in
 *   a controlled test environment. It auto-advances virtual time and
 *   fails if any coroutine throws an unhandled exception.
 *
 * ## Why `src/test/` (not `src/androidTest/`)
 *
 * These tests run on the JVM — no Android device or emulator needed.
 * ViewModels are pure Kotlin classes (they don't touch Android UI APIs),
 * so they can be tested fast and cheap on your development machine.
 * The `src/test/` directory is for JVM tests; `src/androidTest/` is for
 * tests that need the Android runtime (like Room DAO tests).
 */
class EventsViewModelTest {

    /**
     * @get:Rule tells JUnit to apply this rule around every @Test method.
     * It calls `starting()` before and `finished()` after each test.
     */
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: TestCalendarRepository
    private lateinit var viewModel: EventsViewModel

    /**
     * @Before runs before EACH test method, giving every test a fresh
     * ViewModel and repository. Tests should never share mutable state.
     */
    @Before
    fun setup() {
        repository = TestCalendarRepository()
        viewModel = EventsViewModel(repository)
    }

    @Test
    fun `initial state has empty event list`() = runTest {
        // Before any data is emitted, the ViewModel's stateIn initialValue
        // (emptyList()) should be the current state.
        val events = viewModel.events.value
        assertTrue(events.isEmpty())
    }

    @Test
    fun `future events are shown`() = runTest {
        // Create an event that starts tomorrow — should pass the filter
        val futureEvent = testCalendarEvent(id = "future")

        repository.emit(listOf(futureEvent))

        val events = viewModel.events.first { it.isNotEmpty() }
        assertEquals(1, events.size)
        assertEquals("future", events[0].id)
    }

    @Test
    fun `past events are filtered out`() = runTest {
        // An event from 2020 should be excluded by the "no past events" filter
        val pastEvent = testCalendarEvent(
            id = "past",
            startTime = Instant.parse("2020-06-15T10:00:00Z"),
            endTime = Instant.parse("2020-06-15T12:00:00Z"),
        )
        val futureEvent = testCalendarEvent(id = "future")

        repository.emit(listOf(pastEvent, futureEvent))

        val events = viewModel.events.first { it.isNotEmpty() }
        assertEquals(1, events.size)
        assertEquals("future", events[0].id)
    }

    @Test
    fun `search query filters by title`() = runTest {
        val musicEvent = testCalendarEvent(id = "1", title = "Music Festival")
        val scienceEvent = testCalendarEvent(id = "2", title = "Science Fair")

        repository.emit(listOf(musicEvent, scienceEvent))

        // Wait for initial data
        viewModel.events.first { it.size == 2 }

        // Now apply a search filter
        viewModel.setSearchQuery("music")

        val filtered = viewModel.events.first { it.size == 1 }
        assertEquals("Music Festival", filtered[0].title)
    }

    @Test
    fun `search query filters by location`() = runTest {
        val event1 = testCalendarEvent(id = "1", title = "Event A", location = "Grand Hall")
        val event2 = testCalendarEvent(id = "2", title = "Event B", location = "Room 101")

        repository.emit(listOf(event1, event2))
        viewModel.events.first { it.size == 2 }

        viewModel.setSearchQuery("grand")

        val filtered = viewModel.events.first { it.size == 1 }
        assertEquals("Grand Hall", filtered[0].location)
    }

    @Test
    fun `search is case insensitive`() = runTest {
        val event = testCalendarEvent(id = "1", title = "UPPERCASE Title")

        repository.emit(listOf(event))
        viewModel.events.first { it.isNotEmpty() }

        viewModel.setSearchQuery("uppercase")

        val filtered = viewModel.events.first { it.isNotEmpty() }
        assertEquals(1, filtered.size)
    }

    @Test
    fun `clearing search shows all future events again`() = runTest {
        val event1 = testCalendarEvent(id = "1", title = "Music")
        val event2 = testCalendarEvent(id = "2", title = "Science")

        repository.emit(listOf(event1, event2))
        viewModel.events.first { it.size == 2 }

        viewModel.setSearchQuery("music")
        viewModel.events.first { it.size == 1 }

        viewModel.setSearchQuery("")
        val all = viewModel.events.first { it.size == 2 }
        assertEquals(2, all.size)
    }

    @Test
    fun `category filter shows matching events`() = runTest {
        val alumniEvent = testCalendarEvent(
            id = "1",
            categories = listOf(EventCategory.ALUMNI),
        )
        val staffEvent = testCalendarEvent(
            id = "2",
            categories = listOf(EventCategory.STAFF),
        )

        repository.emit(listOf(alumniEvent, staffEvent))
        viewModel.events.first { it.size == 2 }

        viewModel.setCategory(EventCategory.ALUMNI)

        val filtered = viewModel.events.first { it.size == 1 }
        assertEquals(EventCategory.ALUMNI, filtered[0].primaryCategory)
    }

    @Test
    fun `null category shows all events`() = runTest {
        val event1 = testCalendarEvent(id = "1", categories = listOf(EventCategory.ALUMNI))
        val event2 = testCalendarEvent(id = "2", categories = listOf(EventCategory.STAFF))

        repository.emit(listOf(event1, event2))
        viewModel.events.first { it.size == 2 }

        // Set a filter, then clear it
        viewModel.setCategory(EventCategory.ALUMNI)
        viewModel.events.first { it.size == 1 }

        viewModel.setCategory(null)
        val all = viewModel.events.first { it.size == 2 }
        assertEquals(2, all.size)
    }

    @Test
    fun `multi-category event matches any of its categories`() = runTest {
        // An event tagged with both ALUMNI and STAFF should appear when
        // filtering by either category
        val multiCatEvent = testCalendarEvent(
            id = "1",
            categories = listOf(EventCategory.ALUMNI, EventCategory.STAFF),
        )

        repository.emit(listOf(multiCatEvent))
        viewModel.events.first { it.isNotEmpty() }

        viewModel.setCategory(EventCategory.STAFF)
        val filtered = viewModel.events.first { it.isNotEmpty() }
        assertEquals(1, filtered.size)
    }

    @Test
    fun `search and category filter combine`() = runTest {
        val musicAlumni = testCalendarEvent(
            id = "1",
            title = "Music Night",
            categories = listOf(EventCategory.ALUMNI),
        )
        val scienceAlumni = testCalendarEvent(
            id = "2",
            title = "Science Talk",
            categories = listOf(EventCategory.ALUMNI),
        )
        val musicStaff = testCalendarEvent(
            id = "3",
            title = "Music Workshop",
            categories = listOf(EventCategory.STAFF),
        )

        repository.emit(listOf(musicAlumni, scienceAlumni, musicStaff))
        viewModel.events.first { it.size == 3 }

        // Filter by "music" AND Alumni — only musicAlumni should match
        viewModel.setSearchQuery("music")
        viewModel.setCategory(EventCategory.ALUMNI)

        val filtered = viewModel.events.first { it.size == 1 }
        assertEquals("Music Night", filtered[0].title)
    }

    @Test
    fun `toggleBookmark delegates to repository`() = runTest {
        viewModel.toggleBookmark("event-42")

        // Verify the repository received the call
        assertEquals(listOf("event-42"), repository.toggledBookmarkIds)
    }
}
