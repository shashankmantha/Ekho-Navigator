package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.data.social.SocialUser
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.model.OnlineStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = false,
    val user: SocialUser? = null,
    val isOnline: Boolean = false,
    val onlineStatus: OnlineStatus = OnlineStatus.ONLINE,
    val lastSeen: Long = 0L,
    val errorMessage: String? = null,
    val isFriend: Boolean = false,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val presenceRepository: PresenceRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    user = null,
                    errorMessage = null,
                )
            }

            launch {
                repository.observeUser(userId).collect { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }
            }

            try {
                val currentUserId = authRepository.getCurrentUserUid()
                val friends = if (currentUserId != null) repository.getFriends(currentUserId) else emptyList()
                val isFriend = friends.any { it.uid == userId }

                _uiState.update {
                    it.copy(
                        isFriend = isFriend,
                    )
                }

                observePresence(userId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load profile",
                    )
                }
            }
        }
    }

    fun removeFriend(friendUserId: String, onSuccess: () -> Unit) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        viewModelScope.launch {
            try {
                repository.removeFriend(currentUserId, friendUserId)
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to remove friend") }
            }
        }
    }

    private fun observePresence(userId: String) {
        viewModelScope.launch {
            presenceRepository.observePresence(userId).collect { status ->
                val statusStr = status.state.uppercase()
                val onlineStatus = try {
                    OnlineStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    OnlineStatus.ONLINE
                }
                _uiState.update {
                    it.copy(
                        isOnline = status.state != "offline",
                        onlineStatus = onlineStatus,
                        lastSeen = status.lastChanged
                    )
                }
            }
        }
    }
}
