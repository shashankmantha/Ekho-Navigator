package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.data.social.ChatConversation
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
    val conversations: List<ConversationUiModel> = emptyList(),
    val outgoingRequestIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

data class ConversationUiModel(
    val conversationId: String,
    val title: String,
    val avatarId: String = "",
    val isGroup: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val directFriendUserId: String = "",
    val directFriendDisplayName: String = "",
    val directFriendAvatarId: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastSenderId: String = "",
    val hasUnreadMessages: Boolean = false,
    val unreadCount: Int = 0,
    val online: Boolean = false,
    val onlineStatus: OnlineStatus = OnlineStatus.ONLINE,
    val showOnlineStatus: Boolean = true,
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
        observeSignedInUser()
        observeSearchQuery()
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
            stopObservingSocialData()
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

    fun sendFriendRequest(targetUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            stopObservingSocialData()
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.sendFriendRequest(currentUserId, targetUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to send friend request",
                    )
                }
            }
        }
    }

    fun acceptFriendRequest(fromUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            stopObservingSocialData()
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.acceptFriendRequest(currentUserId, fromUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to accept friend request",
                    )
                }
            }
        }
    }

    fun denyFriendRequest(fromUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            stopObservingSocialData()
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.denyFriendRequest(currentUserId, fromUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to deny friend request",
                    )
                }
            }
        }
    }

    fun removeFriend(friendUserId: String) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            stopObservingSocialData()
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.removeFriend(currentUserId, friendUserId)
            }.onSuccess {
                loadSocialData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to remove friend",
                    )
                }
            }
        }
    }

    private fun observeSignedInUser() {
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
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    val trimmedQuery = query.trim()

                    if (!_uiState.value.isSignedIn) {
                        clearSearchResults()
                        return@collect
                    }

                    if (trimmedQuery.length < 2) {
                        clearSearchResults()
                    } else {
                        searchUsers(trimmedQuery)
                    }
                }
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
                val friendsWithChat = attachDirectConversationDataToFriends(
                    currentUserId = userId,
                    friends = friends,
                    conversations = conversations,
                )

                val conversationUiModels = buildConversationUiModels(
                    currentUserId = userId,
                    friends = friends,
                    conversations = conversations,
                )

                SocialDataSnapshot(
                    requests = requests,
                    friends = friendsWithChat,
                    conversations = conversationUiModels,
                )
            }.flatMapLatest { snapshot ->
                observePresenceForSnapshot(snapshot)
            }.catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error observing social data: ${error.message}",
                    )
                }
            }.collect { snapshot ->
                val outgoingRequestIds = repository.getOutgoingRequestIds(userId)

                _uiState.update {
                    it.copy(
                        isSignedIn = true,
                        isLoading = false,
                        incomingRequests = snapshot.requests,
                        friends = snapshot.friends,
                        conversations = snapshot.conversations,
                        outgoingRequestIds = outgoingRequestIds,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    private fun attachDirectConversationDataToFriends(
        currentUserId: String,
        friends: List<FriendUser>,
        conversations: List<ChatConversation>,
    ): List<FriendUser> {
        val directConversationMap = conversations
            .filter { conversation ->
                !conversation.isGroup
            }
            .associateBy { conversation ->
                conversation.otherParticipantId(currentUserId)
            }

        return friends.map { friend ->
            val conversation = directConversationMap[friend.uid] ?: return@map friend
            val isUnread = conversation.isUnreadFor(currentUserId)

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
        }
    }

    private fun buildConversationUiModels(
        currentUserId: String,
        friends: List<FriendUser>,
        conversations: List<ChatConversation>,
    ): List<ConversationUiModel> {
        val friendsById = friends.associateBy { friend ->
            friend.uid
        }

        return conversations
            .filter { conversation ->
                conversation.lastMessage.isNotBlank() || conversation.isUnreadFor(currentUserId)
            }
            .map { conversation ->
                conversation.toConversationUiModel(
                    currentUserId = currentUserId,
                    friendsById = friendsById,
                )
            }
            .sortedWith(
                compareByDescending<ConversationUiModel> {
                    it.hasUnreadMessages
                }.thenByDescending {
                    it.lastMessageTimestamp
                },
            )
    }

    private fun observePresenceForSnapshot(
        snapshot: SocialDataSnapshot,
    ) = if (snapshot.friends.isEmpty()) {
        flowOf(snapshot)
    } else {
        val presenceFlows = snapshot.friends.map { friend ->
            presenceRepository.observePresence(friend.uid).map { presence ->
                val onlineStatus = presence.state.toOnlineStatus()

                friend.copy(
                    online = presence.state != "offline",
                    onlineStatus = onlineStatus,
                    lastChanged = presence.lastChanged,
                )
            }
        }

        combine(presenceFlows) { updatedFriends ->
            val friends = updatedFriends.toList()

            snapshot.copy(
                friends = friends,
                conversations = snapshot.conversations.map { conversation ->
                    if (conversation.isGroup) {
                        conversation
                    } else {
                        val friend = friends.firstOrNull {
                            it.uid == conversation.directFriendUserId
                        }

                        conversation.copy(
                            online = friend?.online ?: false,
                            onlineStatus = friend?.onlineStatus ?: OnlineStatus.ONLINE,
                            showOnlineStatus = friend?.showOnlineStatus ?: true,
                            directFriendAvatarId = friend?.avatarId.orEmpty(),
                            avatarId = friend?.avatarId.orEmpty(),
                        )
                    }
                },
            )
        }
    }

    private fun searchUsers(query: String) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            stopObservingSocialData()
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
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        users = emptyList(),
                        errorMessage = error.message ?: "Failed to search users",
                    )
                }
            }
        }
    }

    private fun clearSearchResults() {
        _uiState.update {
            it.copy(
                isLoading = false,
                users = emptyList(),
                errorMessage = null,
            )
        }
    }

    private fun stopObservingSocialData() {
        observationJob?.cancel()
        observationJob = null

        _uiState.value = SocialUiState(
            isSignedIn = false,
        )

        queryFlow.value = ""
    }

    private fun ChatConversation.toConversationUiModel(
        currentUserId: String,
        friendsById: Map<String, FriendUser>,
    ): ConversationUiModel {
        val directFriendUserId = if (isGroup) {
            ""
        } else {
            otherParticipantId(currentUserId)
        }

        val directFriend = friendsById[directFriendUserId]

        val conversationTitle = when {
            isGroup && title.isNotBlank() -> title

            isGroup -> participantNames
                .filterKeys { participantId ->
                    participantId != currentUserId
                }
                .values
                .joinToString(", ")
                .ifBlank { "Group Chat" }

            directFriend != null -> directFriend.displayName
            directFriendUserId.isNotBlank() -> participantNames[directFriendUserId].orEmpty()
            else -> "Chat"
        }

        val isUnread = isUnreadFor(currentUserId)

        return ConversationUiModel(
            conversationId = id,
            title = conversationTitle,
            avatarId = if (isGroup) "" else directFriend?.avatarId.orEmpty(),
            isGroup = isGroup,
            participantIds = participantIds,
            participantNames = participantNames,
            directFriendUserId = directFriendUserId,
            directFriendDisplayName = if (isGroup) "" else conversationTitle,
            directFriendAvatarId = if (isGroup) "" else directFriend?.avatarId.orEmpty(),
            lastMessage = lastMessage,
            lastMessageTimestamp = lastTimestamp,
            lastSenderId = lastSenderId,
            hasUnreadMessages = isUnread,
            unreadCount = if (isUnread) {
                unreadCount.coerceAtLeast(1)
            } else {
                0
            },
            online = directFriend?.online ?: false,
            onlineStatus = directFriend?.onlineStatus ?: OnlineStatus.ONLINE,
            showOnlineStatus = directFriend?.showOnlineStatus ?: true,
        )
    }

    private fun ChatConversation.otherParticipantId(
        currentUserId: String,
    ): String {
        return participantIds.firstOrNull { participantId ->
            participantId != currentUserId
        }.orEmpty()
    }

    private fun ChatConversation.isUnreadFor(
        currentUserId: String,
    ): Boolean {
        return lastSenderId.isNotBlank() &&
                lastSenderId != currentUserId &&
                (
                        unreadCount > 0 ||
                                currentUserId !in readBy
                        )
    }

    private fun String.toOnlineStatus(): OnlineStatus {
        return try {
            OnlineStatus.valueOf(uppercase())
        } catch (e: Exception) {
            OnlineStatus.ONLINE
        }
    }

    private data class SocialDataSnapshot(
        val requests: List<FriendRequest>,
        val friends: List<FriendUser>,
        val conversations: List<ConversationUiModel>,
    )

    fun createGroupChat(
        groupTitle: String,
        selectedFriends: List<com.ekhonavigator.core.data.social.FriendUser>,
        onCreated: (ConversationUiModel) -> Unit,
    ) {
        val currentUserId = authRepository.getCurrentUserUid()

        if (currentUserId == null) {
            stopObservingSocialData()
            return
        }

        if (selectedFriends.size < 2) {
            _uiState.update {
                it.copy(errorMessage = "Select at least 2 friends to make a group chat.")
            }
            return
        }

        val currentUserName = authRepository.getCurrentUserDisplayName() ?: "Unknown"

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                val participantNames = selectedFriends.associate { friend ->
                    friend.uid to friend.displayName
                }

                val conversation = chatRepository.createGroupConversation(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    groupTitle = groupTitle,
                    participantNames = participantNames,
                )

                val friendsById = selectedFriends.associateBy { friend ->
                    friend.uid
                }

                conversation.toConversationUiModel(
                    currentUserId = currentUserId,
                    friendsById = friendsById,
                )
            }.onSuccess { conversation ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                    )
                }

                onCreated(conversation)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to create group chat",
                    )
                }
            }
        }
    }
}