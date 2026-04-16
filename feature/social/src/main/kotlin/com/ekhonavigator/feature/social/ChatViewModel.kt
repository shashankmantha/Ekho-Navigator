package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.social.ChatMessage
import com.ekhonavigator.core.data.social.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.flow.catch

data class ChatUiState(
    val isLoading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val draftMessage: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var startedConversationId: String? = null

    init {
        viewModelScope.launch {
            authRepository.userFlow().collect { uid ->
                if (uid == null) {
                    observeJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = emptyList(),
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    fun startConversation(
        friendUserId: String,
        friendDisplayName: String,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        val currentUserName = authRepository.getCurrentUserDisplayName() ?: "Unknown"

        viewModelScope.launch {
            runCatching {
                chatRepository.getOrCreateConversation(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    friendUserId = friendUserId,
                    friendDisplayName = friendDisplayName,
                )
            }.onSuccess { conversation ->
                if (startedConversationId == conversation.id) return@onSuccess

                startedConversationId = conversation.id

                observeJob?.cancel()
                observeJob = viewModelScope.launch {
                    chatRepository.observeMessages(conversation.id)
                        .catch { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = e.message ?: "Chat unavailable",
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

                            runCatching {
                                chatRepository.markMessagesAsRead(
                                    conversationId = conversation.id,
                                    currentUserId = currentUserId,
                                )
                            }
                        }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to open chat",
                    )
                }
            }
        }
    }

    fun onDraftMessageChange(value: String) {
        _uiState.update { it.copy(draftMessage = value) }
    }

    fun sendMessage(
        friendUserId: String,
        friendDisplayName: String,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        val currentUserName = authRepository.getCurrentUserDisplayName() ?: "Unknown"
        val draft = uiState.value.draftMessage.trim()

        if (draft.isBlank()) return

        val clientMessageId = UUID.randomUUID().toString()

        viewModelScope.launch {
            runCatching {
                val conversation = chatRepository.getOrCreateConversation(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    friendUserId = friendUserId,
                    friendDisplayName = friendDisplayName,
                )

                chatRepository.sendMessage(
                    conversationId = conversation.id,
                    senderId = currentUserId,
                    senderName = currentUserName,
                    text = draft,
                    clientMessageId = clientMessageId,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        draftMessage = "",
                        errorMessage = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to send message",
                    )
                }
            }
        }
    }

    fun getCurrentUserId(): String? = authRepository.getCurrentUserUid()
}