package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
    private val placeRepository: PlaceRepository,
) : ViewModel() {

    private val _eventId = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val event: StateFlow<CalendarEvent?> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> repository.observeEventById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val attendees: StateFlow<List<EventAttendee>> = _eventId
        .filter { it.isNotEmpty() }
        .flatMapLatest { id -> customEventRepository.observeAttendees(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Resolves to the event's `placeId` only while the place still exists in the repository.
     *  Drops to null when a user marker the event was pinned to has since been deleted, so the
     *  WHERE row gracefully degrades to its un-linked styling instead of dangling on the dead id. */
    val effectivePlaceId: StateFlow<String?> = combine(event, placeRepository.observePlaces()) { ev, places ->
        val pid = ev?.placeId ?: return@combine null
        if (places.any { it.id == pid }) pid else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Current user's RSVP status for this event, or null if not an attendee. */
    val currentUserRsvp: StateFlow<RsvpStatus?> = attendees
        .map { list ->
            val uid = authRepository.getCurrentUserUid() ?: return@map null
            list.find { it.userId == uid }?.rsvpStatus
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _friends = MutableStateFlow<List<FriendUser>>(emptyList())
    val friends: StateFlow<List<FriendUser>> = _friends.asStateFlow()

    private val _shareSheetVisible = MutableStateFlow(false)
    val shareSheetVisible: StateFlow<Boolean> = _shareSheetVisible.asStateFlow()

    val canDelete: Boolean
        get() {
            val event = event.value ?: return false
            val uid = authRepository.getCurrentUserUid() ?: return false
            return event.source == EventSource.USER_CREATED && event.ownerUid == uid
        }

    /** True for any custom event with attendees — shows attendee section for both owner and invitees. */
    val hasAttendees: Boolean
        get() {
            val e = event.value ?: return false
            return e.source == EventSource.SHARED || (e.source == EventSource.USER_CREATED && attendees.value.isNotEmpty())
        }

    /** True when the current user is an invitee (not the owner) — shows RSVP buttons. */
    val canRsvp: Boolean
        get() = event.value?.source == EventSource.SHARED

    val isOwner: Boolean
        get() {
            val e = event.value ?: return false
            val uid = authRepository.getCurrentUserUid() ?: return false
            return e.ownerUid == uid
        }

    val canShare: Boolean
        get() = isOwner && event.value?.source == EventSource.USER_CREATED

    fun setEventId(id: String) {
        _eventId.value = id
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.syncAttendees(id)
            }
        }
    }

    fun toggleBookmark() {
        val id = _eventId.value
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                repository.toggleBookmark(id)
            }
        }
    }

    fun rsvp(status: RsvpStatus) {
        val id = _eventId.value
        val uid = authRepository.getCurrentUserUid() ?: return
        val displayName = authRepository.getCurrentUserDisplayName() ?: ""
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.rsvp(id, uid, displayName, status)
            }
        }
    }

    fun deleteEvent() {
        val id = _eventId.value
        if (id.isNotEmpty()) {
            viewModelScope.launch {
                customEventRepository.deleteEvent(id)
                // No explicit navigation — the Room Flow emits null after delete,
                // which the screen's hadEvent detection catches and calls onBack().
            }
        }
    }

    fun openShareSheet() {
        if (!canShare) return
        loadFriendsIfNeeded()
        _shareSheetVisible.value = true
    }

    fun dismissShareSheet() {
        _shareSheetVisible.value = false
    }

    fun shareWith(newUids: Set<String>) {
        val eventId = _eventId.value.takeIf { it.isNotEmpty() } ?: return
        val toAdd = _friends.value
            .filter { it.uid in newUids }
            .associate { it.uid to it.displayName }
        _shareSheetVisible.value = false
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            customEventRepository.addAttendees(eventId, toAdd)
        }
    }

    private fun loadFriendsIfNeeded() {
        if (_friends.value.isNotEmpty()) return
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                _friends.value = socialRepository.getFriends(uid)
            } catch (_: Exception) {
                // Friends list unavailable — sheet just shows empty state
            }
        }
    }
}
