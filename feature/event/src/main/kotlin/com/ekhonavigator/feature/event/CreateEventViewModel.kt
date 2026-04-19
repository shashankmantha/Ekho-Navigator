package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class CreateEventUiState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

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

    fun setTitle(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun setDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun setLocation(value: String) {
        _uiState.update { it.copy(location = value) }
    }

    fun setDate(value: LocalDate) {
        _uiState.update { it.copy(date = value) }
    }

    fun setStartTime(value: LocalTime) {
        _uiState.update { it.copy(startTime = value) }
    }

    fun setEndTime(value: LocalTime) {
        _uiState.update { it.copy(endTime = value) }
    }

    fun setCategory(value: EventCategory) {
        _uiState.update { it.copy(category = value) }
    }

    fun toggleFriend(uid: String) {
        _uiState.update { state ->
            val current = state.selectedFriendUids
            state.copy(
                selectedFriendUids = if (uid in current) current - uid else current + uid,
            )
        }
    }

    fun save() {
        val ownerUid = authRepository.getCurrentUserUid() ?: return
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }

        val zone = ZoneId.of("America/Los_Angeles")
        val startInstant = state.date!!.atTime(state.startTime!!).atZone(zone).toInstant()
        val endInstant = state.date.atTime(state.endTime!!).atZone(zone).toInstant()

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
        )

        _uiState.update { it.copy(isSaving = true) }

        // Build uid → displayName map from selected friends
        val sharedWith = state.friends
            .filter { it.uid in state.selectedFriendUids }
            .associate { it.uid to it.displayName }

        viewModelScope.launch {
            customEventRepository.createEvent(event, sharedWith)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
