package com.ekhonavigator.feature.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InvitesActionViewModel @Inject constructor(
    calendarRepository: CalendarRepository,
    socialRepository: SocialRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    val pendingCount: StateFlow<Int> = run {
        val uid = authRepository.getCurrentUserUid()
        val friendRequests: Flow<List<FriendRequest>> = if (uid == null) {
            MutableStateFlow<List<FriendRequest>>(emptyList()).asStateFlow()
        } else {
            socialRepository.observeIncomingRequests(uid)
        }
        combine(
            calendarRepository.observePendingInvites(),
            friendRequests,
        ) { invites, requests -> invites.size + requests.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
}
