package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.RsvpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvitesViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _showPast = MutableStateFlow(false)
    val showPast: StateFlow<Boolean> = _showPast.asStateFlow()

    val pendingInvites: StateFlow<List<CalendarEvent>> = _showPast
        .flatMapLatest { calendarRepository.observePendingInvites(includePast = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val declinedInvites: StateFlow<List<CalendarEvent>> = _showPast
        .flatMapLatest { calendarRepository.observeDeclinedInvites(includePast = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val friendRequests: StateFlow<List<FriendRequest>> = run {
        val uid = authRepository.getCurrentUserUid()
        if (uid == null) {
            MutableStateFlow(emptyList())
        } else {
            socialRepository.observeIncomingRequests(uid)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }
    }

    fun togglePast() {
        _showPast.value = !_showPast.value
    }

    fun rsvp(eventId: String, status: RsvpStatus) {
        val uid = authRepository.getCurrentUserUid() ?: return
        val displayName = authRepository.getCurrentUserDisplayName() ?: ""
        viewModelScope.launch {
            customEventRepository.rsvp(eventId, uid, displayName, status)
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            socialRepository.acceptFriendRequest(uid, fromUserId)
        }
    }

    fun denyFriendRequest(fromUserId: String) {
        val uid = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            socialRepository.denyFriendRequest(uid, fromUserId)
        }
    }
}
