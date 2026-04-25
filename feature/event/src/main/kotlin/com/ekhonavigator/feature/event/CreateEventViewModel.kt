package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.place.PlaceRepository
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
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    val locationSuggestions: StateFlow<List<LocationSuggestion>> = placeRepository
        .observePlaces()
        .map { places -> places.map { it.toSuggestion() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadFriends()
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

        val sharedWith = state.friends
            .filter { it.uid in state.selectedFriendUids }
            .associate { it.uid to it.displayName }

        viewModelScope.launch {
            // Last-chance resolution: if the user typed a known place name without
            // tapping the suggestion, still pin the event to that place id.
            val resolvedPlaceId = state.placeId
                ?: state.location.takeIf { it.isNotBlank() }
                    ?.let { placeRepository.resolveFromText(it) }

            val event = CalendarEvent(
                id = "",
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

            customEventRepository.createEvent(event, sharedWith)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

private fun Place.toSuggestion(): LocationSuggestion = LocationSuggestion(
    id = id,
    name = name,
    isCustom = isCustom,
    subtitle = if (isCustom) "Your marker" else "",
)
