package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.data.social.SocialUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.model.OnlineStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class SocialUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val users: List<SocialUser> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val friends: List<FriendUser> = emptyList(),
    val outgoingRequestIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val authRepository: AuthRepository,
    private val presenceRepository: PresenceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadSocialData()

        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    val trimmed = query.trim()

                    if (trimmed.length < 2) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                users = emptyList(),
                                errorMessage = null,
                            )
                        }
                    } else {
                        searchUsers(trimmed)
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        queryFlow.value = query
    }

    fun loadSocialData() {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            runCatching {
                val requests = repository.getIncomingRequests(currentUserId)
                val friends = repository.getFriends(currentUserId)
                val outgoingRequestIds = repository.getOutgoingRequestIds(currentUserId)
                Triple(requests, friends, outgoingRequestIds)
            }.onSuccess { (requests, friends, outgoingRequestIds) ->
                _uiState.update {
                    it.copy(
                        incomingRequests = requests,
                        friends = friends,
                        outgoingRequestIds = outgoingRequestIds,
                        errorMessage = null,
                    )
                }
                observeFriendPresence(friends)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to load social data",
                    )
                }
            }
        }
    }

    private fun observeFriendPresence(friends: List<FriendUser>) {
        if (friends.isEmpty()) return

        viewModelScope.launch {
            val presenceFlows = friends.map { friend ->
                presenceRepository.observePresence(friend.uid).map { presence ->
                    friend.uid to presence
                }
            }

            combine(presenceFlows) { friendPresences ->
                friendPresences.toMap()
            }.collectLatest { presenceMap ->
                _uiState.update { state ->
                    val updatedFriends = state.friends.map { friend ->
                        val presence = presenceMap[friend.uid]
                        if (presence != null) {
                            val statusStr = presence.state.uppercase()
                            val onlineStatus = try {
                                OnlineStatus.valueOf(statusStr)
                            } catch (e: Exception) {
                                OnlineStatus.ONLINE
                            }
                            friend.copy(
                                online = presence.state != "offline",
                                onlineStatus = onlineStatus,
                                lastChanged = presence.lastChanged
                            )
                        } else {
                            friend
                        }
                    }
                    state.copy(friends = updatedFriends)
                }
            }
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            runCatching {
                repository.sendFriendRequest(currentUserId, targetUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to send friend request",
                    )
                }
            }
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            runCatching {
                repository.acceptFriendRequest(currentUserId, fromUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to accept friend request",
                    )
                }
            }
        }
    }

    fun denyFriendRequest(fromUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            runCatching {
                repository.denyFriendRequest(currentUserId, fromUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to deny friend request",
                    )
                }
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            runCatching {
                repository.removeFriend(currentUserId, friendUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to remove friend",
                    )
                }
            }
        }
    }


    private fun searchUsers(query: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.searchUsersByName(
                    query = query,
                    currentUserId = authRepository.getCurrentUserUid(),
                )
            }.onSuccess { users ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = users,
                        errorMessage = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = emptyList(),
                        errorMessage = e.message ?: "Failed to search users",
                    )
                }
            }
        }
    }
}
