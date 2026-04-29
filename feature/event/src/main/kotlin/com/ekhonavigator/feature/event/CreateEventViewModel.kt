package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.designsystem.component.LocationSuggestion
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.Place
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
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val category: EventCategory = EventCategory.GENERAL,
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    private var didLoadEvent = false

    val locationSuggestions: StateFlow<List<LocationSuggestion>> = placeRepository
        .observePlaces()
        .map { places -> places.map { it.toSuggestion() } }
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
                    date = zoned.toLocalDate(),
                    startTime = zoned.toLocalTime(),
                    endTime = zonedEnd.toLocalTime(),
                    category = event.categories.firstOrNull() ?: EventCategory.GENERAL,
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

    /** Free-text edit clears any previously-chosen suggestion id — typing past a selected
     *  place implies the user is no longer pinning it to that exact location. */
    fun setLocationText(value: String) = _uiState.update {
        it.copy(location = value, placeId = null)
    }

    fun selectLocationSuggestion(suggestion: LocationSuggestion) = _uiState.update {
        it.copy(location = suggestion.name, placeId = suggestion.id)
    }

    fun useCustomLocationText(value: String) = _uiState.update {
        it.copy(location = value, placeId = null)
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

            val editingId = state.editingEventId
            if (editingId != null) {
                applyEdit(editingId, ownerUid, state, startInstant, endInstant, resolvedPlaceId)
            } else {
                applyCreate(ownerUid, state, startInstant, endInstant, resolvedPlaceId)
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
    ) {
        val event = baseEvent(
            id = eventId,
            ownerUid = ownerUid,
            state = state,
            startInstant = startInstant,
            endInstant = endInstant,
            resolvedPlaceId = resolvedPlaceId,
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
    ): CalendarEvent = CalendarEvent(
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
    )
}

private fun Place.toSuggestion(): LocationSuggestion = LocationSuggestion(
    id = id,
    name = name,
    isCustom = isCustom,
    subtitle = if (isCustom) "Your marker" else "",
)
