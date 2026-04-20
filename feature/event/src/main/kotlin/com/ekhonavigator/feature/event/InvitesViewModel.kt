package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.RsvpStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvitesViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val customEventRepository: CustomEventRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val pendingInvites: StateFlow<List<CalendarEvent>> = calendarRepository
        .observePendingInvites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val declinedInvites: StateFlow<List<CalendarEvent>> = calendarRepository
        .observeDeclinedInvites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun rsvp(eventId: String, status: RsvpStatus) {
        val uid = authRepository.getCurrentUserUid() ?: return
        val displayName = authRepository.getCurrentUserDisplayName() ?: ""
        viewModelScope.launch {
            customEventRepository.rsvp(eventId, uid, displayName, status)
        }
    }
}
