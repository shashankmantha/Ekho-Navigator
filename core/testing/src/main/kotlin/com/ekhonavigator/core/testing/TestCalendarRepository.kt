package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.util.SyncResult
import com.ekhonavigator.core.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Fake [CalendarRepository] for unit tests.
 *
 * ## Why a fake instead of a mock?
 *
 * Mocking frameworks (Mockito, MockK) generate objects at runtime that
 * mimic an interface's method signatures but have no real behaviour. They
 * work, but they make tests harder to read because the "when/then" setup
 * is verbose and scattered across tests.
 *
 * A **fake** is a real implementation with simplified internals. This one
 * uses [MutableSharedFlow] as a manually-controlled pipe: tests call
 * [emit] to push data, and any Flow collector (like a ViewModel) receives
 * it immediately. This makes data flow explicit and easy to reason about.
 *
 * ## How MutableSharedFlow works here
 *
 * Think of [MutableSharedFlow] as a hot event bus:
 * - `emit(list)` sends a list to all current collectors
 * - Collectors (ViewModels doing `.stateIn()` / `.collectAsStateWithLifecycle()`)
 *   receive the emission and update their state
 *
 * In tests: emit data → assert ViewModel state changed correctly.
 *
 * ## Example
 *
 * ```kotlin
 * val repo = TestCalendarRepository()
 * val viewModel = EventsViewModel(repo)
 *
 * repo.emit(listOf(testEvent1, testEvent2))
 *
 * // Now viewModel.events should contain the filtered list
 * ```
 */
class TestCalendarRepository : CalendarRepository {

    /**
     * The backing flow. Call [emit] to send events to collectors.
     * `replay = 1` means late collectors get the most recent emission
     * immediately (otherwise they'd have to wait for the next one).
     */
    private val eventsFlow = MutableSharedFlow<List<CalendarEvent>>(replay = 1)

    /**
     * Tracks bookmark toggle calls so tests can verify the ViewModel
     * called the right method with the right ID.
     */
    val toggledBookmarkIds = mutableListOf<String>()

    /**
     * Push a new event list to all collectors.
     */
    suspend fun emit(events: List<CalendarEvent>) {
        eventsFlow.emit(events)
    }

    override fun observeEvents(): Flow<List<CalendarEvent>> = eventsFlow

    override fun observeBookmarkedEvents(): Flow<List<CalendarEvent>> =
        eventsFlow.map { events -> events.filter { it.isBookmarked } }

    override fun observeEventsByDateRange(
        start: Instant,
        end: Instant,
    ): Flow<List<CalendarEvent>> =
        eventsFlow.map { events ->
            events.filter { it.startTime >= start && it.startTime < end }
        }

    override fun observeEventById(id: String): Flow<CalendarEvent?> =
        eventsFlow.map { events -> events.find { it.id == id } }

    override suspend fun toggleBookmark(eventId: String) {
        toggledBookmarkIds += eventId
    }

    override suspend fun sync(feedUrl: String): SyncResult =
        SyncResult.Success(eventsUpdated = 0)
}
