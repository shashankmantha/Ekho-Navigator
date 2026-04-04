package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val authRepository: AuthRepository,
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

    /** Current user's RSVP status for this event, or null if not an attendee. */
    val currentUserRsvp: StateFlow<RsvpStatus?> = attendees
        .map { list ->
            val uid = authRepository.getCurrentUserUid() ?: return@map null
            list.find { it.userId == uid }?.rsvpStatus
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
                // which the screen's hadEvent detection catches and calls onBack()
            }
        }
    }
}
