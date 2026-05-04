package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.designsystem.component.LocationSuggestion
import com.ekhonavigator.core.designsystem.theme.CourseColorAssigner
import com.ekhonavigator.core.designsystem.theme.normalizeCourseLabel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.SharedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

private val EventZone: ZoneId = ZoneId.of("America/Los_Angeles")

data class CreateEventUiState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val placeId: String? = null,
    /** Captured at suggestion-pick time for custom (user-marker) locations so the save can
     *  denormalize a SharedLocation onto the event — recipients can resolve the place
     *  even though they don't own the source marker. Null for campus or free-text. */
    val pickedCustomLocation: SharedLocation? = null,
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val category: EventCategory = EventCategory.GENERAL,
    /** Free-text course tag (e.g. "COMP-262"). Stays raw in state so the user
     *  sees exactly what they typed; normalized via [normalizeCourseLabel] at
     *  save time. Empty = no course tag. */
    val courseLabel: String = "",
    val friends: List<FriendUser> = emptyList(),
    val selectedFriendUids: Set<String> = emptySet(),
    /** Non-null when editing — drives Save vs Create label, branches the save() path,
     *  and gates the one-shot pre-population so user edits aren't clobbered by Firestore syncs. */
    val editingEventId: String? = null,
    /** Snapshot of attendees at load time, used to compute add/remove diffs on save. */
    val existingAttendees: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val showValidationErrors: Boolean = false,
) {
    val canSave: Boolean
        get() = title.isNotBlank() && date != null && startTime != null && endTime != null &&
            !endBeforeStart

    /** True once both times are picked but end is not strictly after start. Shown immediately (not gated by save attempt). */
    val endBeforeStart: Boolean
        get() = startTime != null && endTime != null && !endTime.isAfter(startTime)

    /** True when the chosen start moment is already behind us. Soft warning, not a hard block — matches Google Calendar. */
    val startsInPast: Boolean
        get() {
            val d = date ?: return false
            val t = startTime ?: return false
            return d.atTime(t).atZone(EventZone).toInstant().isBefore(Instant.now())
        }

    val titleError: Boolean get() = showValidationErrors && title.isBlank()
    val dateError: Boolean get() = showValidationErrors && date == null
    val startTimeError: Boolean get() = showValidationErrors && startTime == null
    val endTimeError: Boolean get() = showValidationErrors && endTime == null
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val placeRepository: PlaceRepository,
    canvasCourseRepository: CanvasCourseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    private var didLoadEvent = false

    val locationSuggestions: StateFlow<List<LocationSuggestion>> = placeRepository
        .observePlaces()
        .map { places -> places.map { it.toSuggestion() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Course-code suggestions for the Course autocomplete field — extracted to
     *  the family-key form (e.g. "COMP-262") so the dropdown shows clean codes
     *  rather than Canvas's verbose `course.code` (which often equals the full
     *  course name). Family-key extraction also collapses lab+lecture sections
     *  (`COMP-262 Sec 001` and `COMP-262 Sec 01L`) into a single suggestion —
     *  they share a course, and a personal event tagged "COMP-262" should match
     *  both via the same family-key palette mapping anyway.
     *  Empty when Canvas isn't connected — field falls back to free-text only. */
    val courseSuggestions: StateFlow<List<String>> = canvasCourseRepository
        .observeCourses()
        .map { courses ->
            courses.map { CourseColorAssigner.familyKey(it.code) }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadFriends()
    }

    /** One-shot load of an existing event for edit mode. Subsequent calls and
     *  remote Firestore updates are ignored so the user's in-progress edits aren't clobbered. */
    fun setEventId(eventId: String) {
        if (didLoadEvent) return
        didLoadEvent = true
        viewModelScope.launch {
            val event = calendarRepository.observeEventById(eventId).first { it != null } ?: return@launch
            val attendees = customEventRepository.observeAttendees(eventId).first()
            val attendeeMap = attendees.associate { it.userId to it.displayName }
            val zoned = event.startTime.atZone(EventZone)
            val zonedEnd = event.endTime.atZone(EventZone)
            _uiState.update {
                it.copy(
                    editingEventId = eventId,
                    title = event.title,
                    description = event.description,
                    location = event.location,
                    placeId = event.placeId,
                    pickedCustomLocation = event.customLocation,
                    date = zoned.toLocalDate(),
                    startTime = zoned.toLocalTime(),
                    endTime = zonedEnd.toLocalTime(),
                    category = event.categories.firstOrNull() ?: EventCategory.GENERAL,
                    courseLabel = event.courseLabel.orEmpty(),
                    selectedFriendUids = attendeeMap.keys,
                    existingAttendees = attendeeMap,
                )
            }
        }
    }

    private fun loadFriends() {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                val friends = socialRepository.getFriends(uid)
                _uiState.update { it.copy(friends = friends) }
            } catch (_: Exception) {
                // Friends list unavailable — sharing section just won't show
            }
        }
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun setDate(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun setStartTime(value: LocalTime) = _uiState.update { it.copy(startTime = value) }
    fun setEndTime(value: LocalTime) = _uiState.update { it.copy(endTime = value) }
    fun setCategory(value: EventCategory) = _uiState.update { it.copy(category = value) }
    fun setCourseLabel(value: String) = _uiState.update { it.copy(courseLabel = value) }

    /** Free-text edit clears any previously-chosen suggestion id — typing past a selected
     *  place implies the user is no longer pinning it to that exact location. */
    fun setLocationText(value: String) = _uiState.update {
        it.copy(location = value, placeId = null, pickedCustomLocation = null)
    }

    fun selectLocationSuggestion(suggestion: LocationSuggestion) = _uiState.update {
        // Local vals avoid the cross-module public-property smart-cast restriction.
        val lat = suggestion.latitude
        val lng = suggestion.longitude
        val custom = if (suggestion.isCustom && lat != null && lng != null) {
            SharedLocation(suggestion.name, lat, lng)
        } else null
        it.copy(location = suggestion.name, placeId = suggestion.id, pickedCustomLocation = custom)
    }

    fun useCustomLocationText(value: String) = _uiState.update {
        it.copy(location = value, placeId = null, pickedCustomLocation = null)
    }

    fun toggleFriend(uid: String) {
        _uiState.update { state ->
            val current = state.selectedFriendUids
            state.copy(
                selectedFriendUids = if (uid in current) current - uid else current + uid,
            )
        }
    }

    fun setSelectedFriends(uids: Set<String>) {
        _uiState.update { it.copy(selectedFriendUids = uids) }
    }

    fun save() {
        val ownerUid = authRepository.getCurrentUserUid() ?: return
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }

        val startInstant = state.date!!.atTime(state.startTime!!).atZone(EventZone).toInstant()
        val endInstant = state.date.atTime(state.endTime!!).atZone(EventZone).toInstant()

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            // Last-chance resolution: if the user typed a known place name without
            // tapping the suggestion, still pin the event to that place id.
            val resolvedPlaceId = state.placeId
                ?: state.location.takeIf { it.isNotBlank() }
                    ?.let { placeRepository.resolveFromText(it) }

            // If the resolution landed on a custom marker but the user never tapped a
            // suggestion (typed the name freehand), fetch the marker's coords now so the
            // event still carries a customLocation snapshot for recipients.
            val resolvedCustomLocation = state.pickedCustomLocation
                ?: resolvedPlaceId
                    ?.takeIf { it.startsWith("marker_") }
                    ?.let { placeRepository.getPlace(it) }
                    ?.let { place ->
                        SharedLocation(place.name, place.latitude, place.longitude)
                    }

            val editingId = state.editingEventId
            if (editingId != null) {
                applyEdit(editingId, ownerUid, state, startInstant, endInstant, resolvedPlaceId, resolvedCustomLocation)
            } else {
                applyCreate(ownerUid, state, startInstant, endInstant, resolvedPlaceId, resolvedCustomLocation)
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private suspend fun applyCreate(
        ownerUid: String,
        state: CreateEventUiState,
        startInstant: Instant,
        endInstant: Instant,
        resolvedPlaceId: String?,
        resolvedCustomLocation: SharedLocation?,
    ) {
        val sharedWith = state.friends
            .filter { it.uid in state.selectedFriendUids }
            .associate { it.uid to it.displayName }

        val event = baseEvent(
            id = "",
            ownerUid = ownerUid,
            state = state,
            startInstant = startInstant,
            endInstant = endInstant,
            resolvedPlaceId = resolvedPlaceId,
            resolvedCustomLocation = resolvedCustomLocation,
        )
        customEventRepository.createEvent(event, sharedWith)
    }

    private suspend fun applyEdit(
        eventId: String,
        ownerUid: String,
        state: CreateEventUiState,
        startInstant: Instant,
        endInstant: Instant,
        resolvedPlaceId: String?,
        resolvedCustomLocation: SharedLocation?,
    ) {
        val event = baseEvent(
            id = eventId,
            ownerUid = ownerUid,
            state = state,
            startInstant = startInstant,
            endInstant = endInstant,
            resolvedPlaceId = resolvedPlaceId,
            resolvedCustomLocation = resolvedCustomLocation,
        )
        customEventRepository.updateEvent(event)

        val toRemove = state.existingAttendees.keys - state.selectedFriendUids
        for (uid in toRemove) {
            customEventRepository.removeAttendee(eventId, uid)
        }

        val toAdd = (state.selectedFriendUids - state.existingAttendees.keys)
            .mapNotNull { uid ->
                state.friends.firstOrNull { it.uid == uid }?.let { uid to it.displayName }
            }
            .toMap()
        if (toAdd.isNotEmpty()) {
            customEventRepository.addAttendees(eventId, toAdd)
        }
    }

    private fun baseEvent(
        id: String,
        ownerUid: String,
        state: CreateEventUiState,
        startInstant: Instant,
        endInstant: Instant,
        resolvedPlaceId: String?,
        resolvedCustomLocation: SharedLocation?,
    ): CalendarEvent {
        // Only attach a SharedLocation snapshot when the event is pinned to a custom marker
        // (placeId namespaced as marker_*). Campus place IDs are stable cross-user, so denormalizing
        // their coords would just be storage waste. On edits, also re-sync the snapshot's title to
        // the current location text so a renamed marker propagates to recipients.
        val customLocation = resolvedCustomLocation
            ?.takeIf { resolvedPlaceId?.startsWith("marker_") == true }
            ?.copy(title = state.location.trim())

        return CalendarEvent(
            id = id,
            title = state.title.trim(),
            description = state.description.trim(),
            location = state.location.trim(),
            startTime = startInstant,
            endTime = endInstant,
            categories = listOf(state.category),
            url = "",
            status = "CONFIRMED",
            isBookmarked = true,
            lastSyncedAt = Instant.now(),
            source = EventSource.USER_CREATED,
            ownerUid = ownerUid,
            ownerDisplayName = authRepository.getCurrentUserDisplayName().orEmpty(),
            placeId = resolvedPlaceId,
            customLocation = customLocation,
            // Normalize at the boundary so storage stays canonical (`comp 262` →
            // `COMP-262`) and the family-key extractor matches consistently.
            courseLabel = normalizeCourseLabel(state.courseLabel),
        )
    }
}

private fun Place.toSuggestion(): LocationSuggestion = LocationSuggestion(
    id = id,
    name = name,
    isCustom = isCustom,
    subtitle = if (isCustom) "Your marker" else "",
    latitude = latitude.takeIf { isCustom },
    longitude = longitude.takeIf { isCustom },
)
