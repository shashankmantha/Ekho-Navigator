package com.ekhonavigator.feature.event

import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.testCalendarEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EventDetailViewModel].
 *
 * ## What's being tested
 *
 * The detail ViewModel takes an event ID (set via [setEventId]) and
 * observes that specific event from the repository. These tests verify:
 * - The event loads correctly after setting the ID
 * - The null/empty state before the ID is set
 * - Bookmark toggling delegates to the repository
 *
 * ## Key pattern: flatMapLatest
 *
 * The ViewModel uses `flatMapLatest` on its event ID flow. This means
 * when you call `setEventId("abc")`, it cancels any previous observation
 * and starts a new one for "abc". In tests, we emit data to the fake
 * repo, set the ID, and verify the ViewModel picks up the right event.
 */
class EventDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: TestCalendarRepository
    private lateinit var viewModel: EventDetailViewModel

    @Before
    fun setup() {
        repository = TestCalendarRepository()
        viewModel = EventDetailViewModel(repository)
    }

    @Test
    fun `initial state is null before event ID is set`() = runTest {
        // Before setEventId is called, the ViewModel should have no event
        assertNull(viewModel.event.value)
    }

    @Test
    fun `setting event ID loads the correct event`() = runTest {
        val event1 = testCalendarEvent(id = "event-1", title = "Event One")
        val event2 = testCalendarEvent(id = "event-2", title = "Event Two")

        repository.emit(listOf(event1, event2))

        viewModel.setEventId("event-2")

        val loaded = viewModel.event.first { it != null }
        assertEquals("Event Two", loaded?.title)
        assertEquals("event-2", loaded?.id)
    }

    @Test
    fun `event updates are reflected automatically`() = runTest {
        val original = testCalendarEvent(id = "e1", title = "Original Title")
        repository.emit(listOf(original))

        viewModel.setEventId("e1")
        val first = viewModel.event.first { it != null }
        assertEquals("Original Title", first?.title)

        // Simulate the repo emitting an updated version (e.g. after re-sync)
        val updated = testCalendarEvent(id = "e1", title = "Updated Title")
        repository.emit(listOf(updated))

        val second = viewModel.event.first { it?.title == "Updated Title" }
        assertEquals("Updated Title", second?.title)
    }

    @Test
    fun `toggleBookmark delegates to repository`() = runTest {
        viewModel.setEventId("event-42")
        viewModel.toggleBookmark()

        assertEquals(listOf("event-42"), repository.toggledBookmarkIds)
    }

    @Test
    fun `toggleBookmark does nothing when no event ID is set`() = runTest {
        // With an empty event ID, toggling should be a no-op
        viewModel.toggleBookmark()

        assertTrue(repository.toggledBookmarkIds.isEmpty())
    }

    @Test
    fun `setting same event ID twice is a no-op`() = runTest {
        // MutableStateFlow deduplicates equal values — setting the same
        // ID again shouldn't cause a new subscription or any side effects
        val event = testCalendarEvent(id = "e1")
        repository.emit(listOf(event))

        viewModel.setEventId("e1")
        viewModel.event.first { it != null }

        // Setting the same ID again — no crash, no change
        viewModel.setEventId("e1")
        assertEquals("e1", viewModel.event.value?.id)
    }
}
