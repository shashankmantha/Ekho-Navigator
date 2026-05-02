package com.ekhonavigator.feature.event

import com.ekhonavigator.core.designsystem.component.LocationSuggestion
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.PlaceCategory
import com.ekhonavigator.core.model.RsvpStatus
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
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CreateEventViewModel].
 *
 * The viewmodel powers both create and edit flows behind one screen — these tests cover
 * the branches the screen depends on (load, capture, save) without exercising the picker
 * UI or Firestore-touching repository internals.
 */
class CreateEventViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: TestAuthRepository
    private lateinit var calendarRepository: TestCalendarRepository
    private lateinit var customEventRepository: TestCustomEventRepository
    private lateinit var socialRepository: TestSocialRepository
    private lateinit var placeRepository: TestPlaceRepository
    private lateinit var viewModel: CreateEventViewModel

    @Before
    fun setup() {
        authRepository = TestAuthRepository(uid = "owner-1", displayName = "Owner")
        calendarRepository = TestCalendarRepository()
        customEventRepository = TestCustomEventRepository()
        socialRepository = TestSocialRepository()
        placeRepository = TestPlaceRepository()
        viewModel = CreateEventViewModel(
            authRepository,
            calendarRepository,
            customEventRepository,
            socialRepository,
            placeRepository,
        )
    }

    @Test
    fun `setEventId pre-populates state from the loaded event`() = runTest {
        val event = testCalendarEvent(
            id = "evt-1",
            title = "Study Session",
            description = "Bring laptop",
            location = "Library",
            placeId = "marker_42",
            customLocation = SharedLocation("Library", 34.16, -119.04),
        )
        calendarRepository.emit(listOf(event))

        viewModel.setEventId("evt-1")
        val state = viewModel.uiState.first { it.editingEventId == "evt-1" }

        assertEquals("Study Session", state.title)
        assertEquals("Bring laptop", state.description)
        assertEquals("Library", state.location)
        assertEquals("marker_42", state.placeId)
        // Existing customLocation is restored so an edit-save still carries the snapshot
        // (recipients of the shared event don't lose the marker just because the owner edited).
        assertEquals(SharedLocation("Library", 34.16, -119.04), state.pickedCustomLocation)
    }

    @Test
    fun `setEventId restores existing attendees as preselected`() = runTest {
        val event = testCalendarEvent(id = "evt-1")
        calendarRepository.emit(listOf(event))
        customEventRepository.setAttendees(
            "evt-1",
            listOf(
                EventAttendee(userId = "alice", displayName = "Alice", rsvpStatus = RsvpStatus.GOING),
                EventAttendee(userId = "bob", displayName = "Bob", rsvpStatus = RsvpStatus.PENDING),
            ),
        )

        viewModel.setEventId("evt-1")
        val state = viewModel.uiState.first { it.editingEventId == "evt-1" }

        assertEquals(setOf("alice", "bob"), state.selectedFriendUids)
        assertEquals(mapOf("alice" to "Alice", "bob" to "Bob"), state.existingAttendees)
    }

    @Test
    fun `setEventId is a one-shot — subsequent calls are ignored`() = runTest {
        val original = testCalendarEvent(id = "evt-1", title = "Original")
        calendarRepository.emit(listOf(original))
        viewModel.setEventId("evt-1")
        viewModel.uiState.first { it.editingEventId == "evt-1" }

        // Even if we point at a different event, the in-progress edits must not be clobbered.
        viewModel.setEventId("evt-2")
        // editingEventId stays "evt-1", title stays "Original"
        assertEquals("evt-1", viewModel.uiState.value.editingEventId)
        assertEquals("Original", viewModel.uiState.value.title)
    }

    @Test
    fun `selectLocationSuggestion captures pickedCustomLocation only for custom markers`() {
        viewModel.selectLocationSuggestion(
            LocationSuggestion(
                id = "marker_7",
                name = "Coffee spot",
                isCustom = true,
                latitude = 34.16,
                longitude = -119.04,
            ),
        )
        assertEquals(
            SharedLocation("Coffee spot", 34.16, -119.04),
            viewModel.uiState.value.pickedCustomLocation,
        )
        assertEquals("marker_7", viewModel.uiState.value.placeId)

        viewModel.selectLocationSuggestion(
            LocationSuggestion(id = "broome_library", name = "Broome Library", isCustom = false),
        )
        // Picking a campus place clears the custom snapshot — campus IDs are stable, no
        // denormalization needed and stale coords would mislead recipients.
        assertNull(viewModel.uiState.value.pickedCustomLocation)
        assertEquals("broome_library", viewModel.uiState.value.placeId)
    }

    @Test
    fun `setLocationText clears placeId and pickedCustomLocation`() {
        viewModel.selectLocationSuggestion(
            LocationSuggestion(
                id = "marker_7",
                name = "Coffee spot",
                isCustom = true,
                latitude = 34.16,
                longitude = -119.04,
            ),
        )
        viewModel.setLocationText("somewhere else")
        assertNull(viewModel.uiState.value.placeId)
        assertNull(viewModel.uiState.value.pickedCustomLocation)
        assertEquals("somewhere else", viewModel.uiState.value.location)
    }

    @Test
    fun `save in create mode delegates to createEvent with selected friends`() = runTest {
        viewModel.setTitle("Pizza Night")
        viewModel.setDate(java.time.LocalDate.of(2030, 6, 1))
        viewModel.setStartTime(java.time.LocalTime.of(18, 0))
        viewModel.setEndTime(java.time.LocalTime.of(20, 0))
        viewModel.selectLocationSuggestion(
            LocationSuggestion(
                id = "marker_7",
                name = "Pizza Place",
                isCustom = true,
                latitude = 34.16,
                longitude = -119.04,
            ),
        )

        viewModel.save()
        viewModel.uiState.first { it.isSaved }

        assertEquals(1, customEventRepository.createdEvents.size)
        val (created, _) = customEventRepository.createdEvents.first()
        assertEquals("Pizza Night", created.title)
        assertEquals("marker_7", created.placeId)
        // Custom-marker save denormalizes the SharedLocation snapshot for recipients.
        assertEquals(SharedLocation("Pizza Place", 34.16, -119.04), created.customLocation)
    }

    @Test
    fun `save in edit mode applies attendee diff and updates event`() = runTest {
        // Friends must be seeded BEFORE the viewmodel is constructed — init{} pulls them once
        // from the social repo, and there's no public re-load path.
        socialRepository.friends = listOf(
            com.ekhonavigator.core.data.social.FriendUser(uid = "alice", displayName = "Alice"),
            com.ekhonavigator.core.data.social.FriendUser(uid = "carol", displayName = "Carol"),
        )
        val freshViewModel = CreateEventViewModel(
            authRepository,
            calendarRepository,
            customEventRepository,
            socialRepository,
            placeRepository,
        )
        freshViewModel.uiState.first { it.friends.isNotEmpty() }

        // Existing event with Alice + Bob; user removes Bob and adds Carol.
        val event = testCalendarEvent(
            id = "evt-1",
            title = "Original",
            startTime = Instant.parse("2030-06-01T18:00:00Z"),
            endTime = Instant.parse("2030-06-01T20:00:00Z"),
            placeId = null,
        )
        calendarRepository.emit(listOf(event))
        customEventRepository.setAttendees(
            "evt-1",
            listOf(
                EventAttendee("alice", "Alice", RsvpStatus.GOING),
                EventAttendee("bob", "Bob", RsvpStatus.PENDING),
            ),
        )
        freshViewModel.setEventId("evt-1")
        freshViewModel.uiState.first { it.editingEventId == "evt-1" }

        freshViewModel.setSelectedFriends(setOf("alice", "carol"))
        freshViewModel.setTitle("Renamed")

        freshViewModel.save()
        freshViewModel.uiState.first { it.isSaved }

        assertEquals(1, customEventRepository.updatedEvents.size)
        assertEquals("Renamed", customEventRepository.updatedEvents.first().title)
        assertEquals(listOf("evt-1" to "bob"), customEventRepository.removedAttendees)
        assertEquals(
            listOf("evt-1" to mapOf("carol" to "Carol")),
            customEventRepository.addedAttendees,
        )
    }

    @Test
    fun `save without title flags validation errors and does not persist`() = runTest {
        viewModel.setDate(java.time.LocalDate.of(2030, 6, 1))
        viewModel.setStartTime(java.time.LocalTime.of(18, 0))
        viewModel.setEndTime(java.time.LocalTime.of(20, 0))
        // No title set.
        viewModel.save()

        assertTrue(viewModel.uiState.value.showValidationErrors)
        assertTrue(customEventRepository.createdEvents.isEmpty())
    }

    @Test
    fun `save resolves typed marker name and packs customLocation via fallback`() = runTest {
        // User typed the marker's name freehand instead of tapping the dropdown — the save
        // path should still resolve the marker and capture its coords for recipients.
        placeRepository.emit(
            listOf(
                Place(
                    id = "marker_7",
                    name = "farm",
                    latitude = 34.16,
                    longitude = -119.04,
                    category = PlaceCategory.GENERAL,
                    isCustom = true,
                ),
            ),
        )
        viewModel.setTitle("Picnic")
        viewModel.setDate(java.time.LocalDate.of(2030, 6, 1))
        viewModel.setStartTime(java.time.LocalTime.of(18, 0))
        viewModel.setEndTime(java.time.LocalTime.of(20, 0))
        viewModel.setLocationText("farm")

        viewModel.save()
        viewModel.uiState.first { it.isSaved }

        val (created, _) = customEventRepository.createdEvents.first()
        assertEquals("marker_7", created.placeId)
        assertEquals(SharedLocation("farm", 34.16, -119.04), created.customLocation)
    }

}
