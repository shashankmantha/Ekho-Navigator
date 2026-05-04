package com.ekhonavigator.feature.event

import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.model.SharedLocation
import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestAuthRepository
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.TestCustomEventRepository
import com.ekhonavigator.core.testing.TestPlaceRepository
import com.ekhonavigator.core.testing.TestSocialRepository
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
    private lateinit var customEventRepository: TestCustomEventRepository
    private lateinit var socialRepository: TestSocialRepository
    private lateinit var authRepository: TestAuthRepository
    private lateinit var placeRepository: TestPlaceRepository
    private lateinit var markerRepository: MarkerRepository
    private lateinit var viewModel: EventDetailViewModel

    @Before
    fun setup() {
        repository = TestCalendarRepository()
        customEventRepository = TestCustomEventRepository()
        socialRepository = TestSocialRepository()
        authRepository = TestAuthRepository()
        placeRepository = TestPlaceRepository()
        // Real MarkerRepository is safe in JVM tests since its FirebaseFirestore handle is
        // lazy — these tests never trigger a save path that would touch it.
        markerRepository = MarkerRepository()
        viewModel = EventDetailViewModel(
            repository,
            customEventRepository,
            socialRepository,
            authRepository,
            placeRepository,
            markerRepository,
            StubPlannerRepository(),
        )
    }

    private class StubPlannerRepository : com.ekhonavigator.core.data.canvas.CanvasPlannerRepository {
        override fun observeItems(start: java.time.Instant, end: java.time.Instant) =
            kotlinx.coroutines.flow.flowOf(emptyList<com.ekhonavigator.core.canvas.model.PlannerItem>())
        override fun observeAllItems() =
            kotlinx.coroutines.flow.flowOf(emptyList<com.ekhonavigator.core.canvas.model.PlannerItem>())
        override fun observeById(id: String) =
            kotlinx.coroutines.flow.flowOf<com.ekhonavigator.core.canvas.model.PlannerItem?>(null)
        override suspend fun sync(start: java.time.Instant, end: java.time.Instant) = Result.success(Unit)
        override suspend fun clearAll() {}
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
    fun `effectivePlaceId surfaces the id only while the place still exists`() = runTest {
        val place = com.ekhonavigator.core.model.Place(
            id = "marker_42",
            name = "Coffee spot",
            latitude = 0.0,
            longitude = 0.0,
            category = com.ekhonavigator.core.model.PlaceCategory.GENERAL,
            isCustom = true,
        )
        placeRepository.emit(listOf(place))
        repository.emit(listOf(testCalendarEvent(id = "e1", placeId = "marker_42")))

        viewModel.setEventId("e1")
        viewModel.event.first { it != null }
        assertEquals("marker_42", viewModel.effectivePlaceId.first { it != null })

        // The user deletes the underlying marker — the link must go dead.
        placeRepository.emit(emptyList())
        assertEquals(null, viewModel.effectivePlaceId.first { it == null })
    }

    @Test
    fun `customLocationOffer is null when the event's marker still resolves`() = runTest {
        val place = com.ekhonavigator.core.model.Place(
            id = "marker_42",
            name = "Coffee spot",
            latitude = 34.16,
            longitude = -119.04,
            category = com.ekhonavigator.core.model.PlaceCategory.GENERAL,
            isCustom = true,
        )
        placeRepository.emit(listOf(place))
        repository.emit(
            listOf(
                testCalendarEvent(
                    id = "e1",
                    placeId = "marker_42",
                    customLocation = SharedLocation("Coffee spot", 34.16, -119.04),
                ),
            ),
        )

        viewModel.setEventId("e1")
        viewModel.event.first { it != null }
        // The marker resolves, so no offer prompt is needed.
        assertEquals(null, viewModel.customLocationOffer.value)
    }

    @Test
    fun `customLocationOffer surfaces when the event's marker is unresolvable`() = runTest {
        val event = testCalendarEvent(
            id = "shared-1",
            placeId = "marker_99",
            customLocation = SharedLocation("Coffee spot", 34.16, -119.04),
        )
        placeRepository.emit(emptyList())
        repository.emit(listOf(event))

        viewModel.setEventId("shared-1")
        val offer = viewModel.customLocationOffer.first { it != null }
        assertEquals("Coffee spot", offer?.title)
        assertEquals(34.16, offer?.latitude)
    }

    @Test
    fun `effectivePlaceId coord-matches a saved marker when ids differ`() = runTest {
        // Recipient already saved the shared customLocation as their own marker — same coords,
        // different local id. The WHERE row must navigate straight to their copy and the offer
        // prompt must stay suppressed instead of asking them to save again on every tap.
        val recipientLocalMarker = com.ekhonavigator.core.model.Place(
            id = "marker_99",
            name = "Coffee spot",
            latitude = 34.16,
            longitude = -119.04,
            category = com.ekhonavigator.core.model.PlaceCategory.GENERAL,
            isCustom = true,
        )
        placeRepository.emit(listOf(recipientLocalMarker))
        repository.emit(
            listOf(
                testCalendarEvent(
                    id = "shared-1",
                    placeId = "marker_42",
                    customLocation = SharedLocation("Coffee spot", 34.16, -119.04),
                ),
            ),
        )

        viewModel.setEventId("shared-1")
        viewModel.event.first { it != null }
        assertEquals("marker_99", viewModel.effectivePlaceId.first { it != null })
        assertEquals(null, viewModel.customLocationOffer.value)
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
