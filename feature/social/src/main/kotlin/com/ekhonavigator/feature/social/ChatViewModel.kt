package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.data.markers.UserDroppedMarker
import com.ekhonavigator.core.data.repository.PresenceRepository
import com.ekhonavigator.core.data.social.ChatMessage
import com.ekhonavigator.core.data.social.ChatRepository
import com.ekhonavigator.core.model.PresenceStatus
import com.ekhonavigator.core.model.SharedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val draftMessage: String = "",
    val pendingSharedLocation: SharedLocation? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val friendPresence: PresenceStatus? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val markerRepository: MarkerRepository,
    private val presenceRepository: PresenceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var observeMessagesJob: Job? = null
    private var observePresenceJob: Job? = null

    private var activeConversationId: String? = null
    private var activeFriendUserId: String = ""
    private var activeFriendDisplayName: String = ""
    private var pendingGroupTitle: String = ""
    private var pendingGroupParticipantNames: Map<String, String> = emptyMap()

    init {
        observeSignedInUser()
    }

    fun startConversation(
        friendUserId: String,
        friendDisplayName: String,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        val currentUserName = authRepository.getCurrentUserDisplayName() ?: "Unknown"

        activeFriendUserId = friendUserId
        activeFriendDisplayName = friendDisplayName

        observeFriendPresence(friendUserId)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            runCatching {
                chatRepository.getOrCreateConversation(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    friendUserId = friendUserId,
                    friendDisplayName = friendDisplayName,
                )
            }.onSuccess { conversation ->
                startObservingMessages(
                    conversationId = conversation.id,
                    currentUserId = currentUserId,
                )
            }.onFailure { error ->
                showOpenChatError(error)
            }
        }
    }

    fun startExistingConversation(
        conversationId: String,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        activeFriendUserId = ""
        activeFriendDisplayName = ""

        observePresenceJob?.cancel()
        _uiState.update {
            it.copy(
                friendPresence = null,
                isLoading = true,
                errorMessage = null,
            )
        }

        startObservingMessages(
            conversationId = conversationId,
            currentUserId = currentUserId,
        )
    }

    fun onDraftMessageChange(value: String) {
        _uiState.update {
            it.copy(draftMessage = value)
        }
    }

    fun stageSharedLocation(location: SharedLocation) {
        _uiState.update {
            it.copy(pendingSharedLocation = location)
        }
    }

    fun clearPendingSharedLocation() {
        _uiState.update {
            it.copy(pendingSharedLocation = null)
        }
    }

    fun dismissInfoMessage() {
        _uiState.update {
            it.copy(infoMessage = null)
        }
    }

    fun sendMessage(
        friendUserId: String = activeFriendUserId,
        friendDisplayName: String = activeFriendDisplayName,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        val currentUserName = authRepository.getCurrentUserDisplayName() ?: "Unknown"

        val currentState = uiState.value
        val draft = currentState.draftMessage.trim()
        val pendingLocation = currentState.pendingSharedLocation

        if (draft.isBlank() && pendingLocation == null) return

        viewModelScope.launch {
            runCatching {
                val conversationId = getActiveConversationId(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    friendUserId = friendUserId,
                    friendDisplayName = friendDisplayName,
                )

                chatRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    text = draft.ifBlank {
                        "Shared a location: ${pendingLocation?.title}"
                    },
                    clientMessageId = UUID.randomUUID().toString(),
                    sharedLocation = pendingLocation,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        draftMessage = "",
                        pendingSharedLocation = null,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                showTemporaryInfoMessage(
                    error.message ?: "Failed to send message",
                )
            }
        }
    }

    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserUid()
    }

    fun saveSharedLocationToMap(
        location: SharedLocation,
        onSaved: () -> Unit,
    ) {
        val userId = authRepository.getCurrentUserUid() ?: return

        viewModelScope.launch {
            runCatching {
                val existingMarkers = markerRepository.getUserMarkers(userId)

                val isDuplicate = existingMarkers.any { marker ->
                    marker.latitude == location.latitude &&
                            marker.longitude == location.longitude
                }

                if (isDuplicate) {
                    showTemporaryInfoMessage("You already have a copy of this marker.")
                    return@runCatching
                }

                val marker = UserDroppedMarker(
                    id = System.currentTimeMillis().toString(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    comment = location.title,
                )

                markerRepository.saveMarker(userId, marker)
                onSaved()
            }.onFailure { error ->
                showTemporaryInfoMessage(
                    "Failed to save marker: ${error.message}",
                )
            }
        }
    }

    private fun observeSignedInUser() {
        viewModelScope.launch {
            authRepository.userFlow().collectLatest { uid ->
                if (uid == null) {
                    stopCurrentConversation()
                }
            }
        }
    }

    private fun observeFriendPresence(friendUserId: String) {
        observePresenceJob?.cancel()

        observePresenceJob = viewModelScope.launch {
            presenceRepository.observePresence(friendUserId)
                .catch {
                    // Presence is optional for chat, so ignore presence errors.
                }
                .collect { presence ->
                    _uiState.update {
                        it.copy(friendPresence = presence)
                    }
                }
        }
    }

    private fun startObservingMessages(
        conversationId: String,
        currentUserId: String,
    ) {
        if (activeConversationId == conversationId && observeMessagesJob?.isActive == true) {
            return
        }

        activeConversationId = conversationId

        observeMessagesJob?.cancel()
        observeMessagesJob = viewModelScope.launch {
            chatRepository.observeMessages(conversationId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Chat unavailable",
                        )
                    }
                }
                .collect { messages ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            errorMessage = null,
                        )
                    }

                    markMessagesAsReadIfNeeded(
                        conversationId = conversationId,
                        currentUserId = currentUserId,
                        messages = messages,
                    )
                }
        }
    }

    private fun markMessagesAsReadIfNeeded(
        conversationId: String,
        currentUserId: String,
        messages: List<ChatMessage>,
    ) {
        if (messages.isEmpty()) return

        viewModelScope.launch {
            runCatching {
                chatRepository.markMessagesAsRead(
                    conversationId = conversationId,
                    currentUserId = currentUserId,
                )
            }
        }
    }

    private suspend fun getActiveConversationId(
        currentUserId: String,
        currentUserName: String,
        friendUserId: String,
        friendDisplayName: String,
    ): String {
        activeConversationId?.let {
            return it
        }

        if (friendUserId.isBlank()) {
            if (pendingGroupParticipantNames.isEmpty()) {
                error("No active conversation selected")
            }

            val conversation = chatRepository.createGroupConversation(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                groupTitle = pendingGroupTitle,
                participantNames = pendingGroupParticipantNames,
            )

            activeConversationId = conversation.id

            pendingGroupTitle = ""
            pendingGroupParticipantNames = emptyMap()

            startObservingMessages(
                conversationId = conversation.id,
                currentUserId = currentUserId,
            )

            return conversation.id
        }

        val conversation = chatRepository.getOrCreateConversation(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            friendUserId = friendUserId,
            friendDisplayName = friendDisplayName,
        )

        activeConversationId = conversation.id
        return conversation.id
    }

    private fun stopCurrentConversation() {
        observeMessagesJob?.cancel()
        observePresenceJob?.cancel()

        activeConversationId = null
        activeFriendUserId = ""
        activeFriendDisplayName = ""

        _uiState.update {
            it.copy(
                isLoading = false,
                messages = emptyList(),
                errorMessage = null,
                friendPresence = null,
            )
        }
    }

    private fun showOpenChatError(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = error.message ?: "Failed to open chat",
            )
        }
    }

    private fun showTemporaryInfoMessage(message: String) {
        _uiState.update {
            it.copy(infoMessage = message)
        }

        viewModelScope.launch {
            delay(INFO_MESSAGE_DURATION_MS)
            _uiState.update {
                it.copy(infoMessage = null)
            }
        }
    }

    companion object {
        private const val INFO_MESSAGE_DURATION_MS = 5_000L
    }

    fun startPendingGroupConversation(
        groupTitle: String,
        participantNames: Map<String, String>,
    ) {
        activeConversationId = null
        activeFriendUserId = ""
        activeFriendDisplayName = ""

        pendingGroupTitle = groupTitle.trim()
        pendingGroupParticipantNames = participantNames

        observeMessagesJob?.cancel()
        observePresenceJob?.cancel()

        _uiState.update {
            it.copy(
                isLoading = false,
                messages = emptyList(),
                errorMessage = null,
                friendPresence = null,
            )
        }
    }
}