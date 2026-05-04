package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.data.social.ChatRepository
import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.data.social.SocialUser
import com.ekhonavigator.core.model.OnlineStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocialUiState(
    val isSignedIn: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val users: List<SocialUser> = emptyList(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val friends: List<FriendUser> = emptyList(),
    val outgoingRequestIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SocialViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val authRepository: AuthRepository,
    private val presenceRepository: PresenceRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var observationJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.userFlow().collectLatest { uid ->
                observationJob?.cancel()
                observationJob = null

                if (uid == null) {
                    _uiState.value = SocialUiState(
                        isSignedIn = false,
                    )
                    queryFlow.value = ""
                    return@collectLatest
                }

                _uiState.update {
                    it.copy(
                        isSignedIn = true,
                        isLoading = false,
                        errorMessage = null,
                    )
                }

                observeSocialData(uid)
            }
        }

        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    val trimmed = query.trim()

                    if (!_uiState.value.isSignedIn) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                users = emptyList(),
                                errorMessage = null,
                            )
                        }
                        return@collect
                    }

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
        _uiState.update {
            it.copy(searchQuery = query)
        }

        queryFlow.value = query
    }

    fun loadSocialData() {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            observationJob?.cancel()
            observationJob = null

            _uiState.value = SocialUiState(
                isSignedIn = false,
            )
            return
        }

        _uiState.update {
            it.copy(
                isSignedIn = true,
                errorMessage = null,
            )
        }

        if (observationJob == null || observationJob?.isActive == false) {
            observeSocialData(currentUserId)
        }
    }

    private fun observeSocialData(userId: String) {
        observationJob?.cancel()

        observationJob = viewModelScope.launch {
            val incomingRequestsFlow = repository.observeIncomingRequests(userId)
            val friendsFlow = repository.observeFriends(userId)
            val conversationsFlow = chatRepository.observeAllConversations(userId)

            combine(
                incomingRequestsFlow,
                friendsFlow,
                conversationsFlow,
            ) { requests, friends, conversations ->
                val conversationMap = conversations.associateBy { conversation ->
                    conversation.participantIds.find { participantId ->
                        participantId != userId
                    } ?: ""
                }

                val friendsWithChat = friends.map { friend ->
                    val conversation = conversationMap[friend.uid]

                    if (conversation != null) {
                        val isUnread =
                            conversation.lastSenderId != userId &&
                                    (
                                            conversation.unreadCount > 0 ||
                                                    !conversation.readBy.contains(userId)
                                            )

                        friend.copy(
                            lastMessage = conversation.lastMessage,
                            lastMessageTimestamp = conversation.lastTimestamp,
                            lastMessageSenderId = conversation.lastSenderId,
                            hasUnreadMessages = isUnread,
                            unreadCount = if (isUnread) {
                                conversation.unreadCount.coerceAtLeast(1)
                            } else {
                                0
                            },
                        )
                    } else {
                        friend
                    }
                }

                requests to friendsWithChat
            }.flatMapLatest { (requests, friends) ->
                if (friends.isEmpty()) {
                    flowOf(requests to emptyList<FriendUser>())
                } else {
                    val presenceFlows = friends.map { friend ->
                        presenceRepository.observePresence(friend.uid).map { presence ->
                            val onlineStatus = try {
                                OnlineStatus.valueOf(presence.state.uppercase())
                            } catch (e: Exception) {
                                OnlineStatus.ONLINE
                            }

                            friend.copy(
                                online = presence.state != "offline",
                                onlineStatus = onlineStatus,
                                lastChanged = presence.lastChanged,
                            )
                        }
                    }

                    combine(presenceFlows) { updatedFriends ->
                        requests to updatedFriends.toList()
                    }
                }
            }.catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error observing social data: ${e.message}",
                    )
                }
            }.collect { (requests, updatedFriends) ->
                val outgoingRequestIds = repository.getOutgoingRequestIds(userId)

                _uiState.update {
                    it.copy(
                        isSignedIn = true,
                        isLoading = false,
                        incomingRequests = requests,
                        friends = updatedFriends,
                        outgoingRequestIds = outgoingRequestIds,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            _uiState.value = SocialUiState(
                isSignedIn = false,
            )
            return
        }

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
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            _uiState.value = SocialUiState(
                isSignedIn = false,
            )
            return
        }

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
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            _uiState.value = SocialUiState(
                isSignedIn = false,
            )
            return
        }

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
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            _uiState.value = SocialUiState(
                isSignedIn = false,
            )
            return
        }

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
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            _uiState.value = SocialUiState(
                isSignedIn = false,
            )
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSignedIn = true,
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                repository.searchUsersByName(
                    query = query,
                    currentUserId = currentUserId,
                )
            }.onSuccess { users ->
                _uiState.update {
                    it.copy(
                        isSignedIn = true,
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