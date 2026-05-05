package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.social.ChatConversation
import com.ekhonavigator.core.data.social.ChatMessage
import com.ekhonavigator.core.data.social.ChatMuteRepository
import com.ekhonavigator.core.data.social.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatOptionsUiState(
    val isLoading: Boolean = true,
    val conversation: ChatConversation? = null,
    val isMuted: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<ChatMessage> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val hasLeftConversation: Boolean = false,
)

@HiltViewModel
class ChatOptionsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMuteRepository: ChatMuteRepository,
    private val authRepository: AuthRepository,
    private val chatFocusRepository: ChatFocusRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatOptionsUiState())
    val uiState: StateFlow<ChatOptionsUiState> = _uiState.asStateFlow()

    private var activeConversationId: String = ""
    private var observeConversationJob: Job? = null
    private var observeMutedJob: Job? = null
    private var searchJob: Job? = null

    fun start(
        conversationId: String,
    ) {
        if (activeConversationId == conversationId) return

        activeConversationId = conversationId

        observeConversation(conversationId)
        observeMuted(conversationId)
    }

    fun onSearchQueryChange(
        value: String,
    ) {
        _uiState.update {
            it.copy(searchQuery = value)
        }

        searchMessages(value)
    }

    fun focusMessage(
        messageId: String,
        onFocused: () -> Unit,
    ) {
        val conversationId = activeConversationId

        if (conversationId.isBlank() || messageId.isBlank()) return

        chatFocusRepository.requestFocus(
            conversationId = conversationId,
            messageId = messageId,
        )

        onFocused()
    }

    fun setMuted(
        muted: Boolean,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        val conversationId = activeConversationId

        if (conversationId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                chatMuteRepository.setConversationMuted(
                    userId = currentUserId,
                    conversationId = conversationId,
                    muted = muted,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to update mute setting",
                    )
                }
            }
        }
    }

    fun renameGroup(
        newTitle: String,
    ) {
        val conversationId = activeConversationId

        if (conversationId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                chatRepository.renameGroupConversation(
                    conversationId = conversationId,
                    newTitle = newTitle,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(infoMessage = "Group name updated")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to rename group",
                    )
                }
            }
        }
    }

    fun addParticipants(
        participantNames: Map<String, String>,
    ) {
        val conversationId = activeConversationId

        if (conversationId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                chatRepository.addParticipantsToGroupConversation(
                    conversationId = conversationId,
                    newParticipantNames = participantNames,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(infoMessage = "Participants added")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to add participants",
                    )
                }
            }
        }
    }

    fun leaveGroup() {
        val currentUserId = authRepository.getCurrentUserUid() ?: return
        val conversationId = activeConversationId

        if (conversationId.isBlank()) return

        viewModelScope.launch {
            runCatching {
                chatRepository.leaveGroupConversation(
                    conversationId = conversationId,
                    currentUserId = currentUserId,
                )
            }.onSuccess {
                _uiState.update {
                    it.copy(hasLeftConversation = true)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to leave group",
                    )
                }
            }
        }
    }

    fun dismissMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                infoMessage = null,
            )
        }
    }

    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserUid()
    }

    private fun observeConversation(
        conversationId: String,
    ) {
        observeConversationJob?.cancel()

        observeConversationJob = viewModelScope.launch {
            chatRepository.observeConversationById(conversationId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load conversation",
                        )
                    }
                }
                .collectLatest { conversation ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            conversation = conversation,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun observeMuted(
        conversationId: String,
    ) {
        val currentUserId = authRepository.getCurrentUserUid() ?: return

        observeMutedJob?.cancel()

        observeMutedJob = viewModelScope.launch {
            chatMuteRepository.observeConversationMuted(
                userId = currentUserId,
                conversationId = conversationId,
            ).catch {
                _uiState.update {
                    it.copy(isMuted = false)
                }
            }.collectLatest { muted ->
                _uiState.update {
                    it.copy(isMuted = muted)
                }
            }
        }
    }

    private fun searchMessages(
        query: String,
    ) {
        val conversationId = activeConversationId

        searchJob?.cancel()

        if (conversationId.isBlank() || query.isBlank()) {
            _uiState.update {
                it.copy(searchResults = emptyList())
            }
            return
        }

        searchJob = viewModelScope.launch {
            try {
                val results = chatRepository.searchMessages(
                    conversationId = conversationId,
                    query = query,
                )

                _uiState.update {
                    it.copy(searchResults = results)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "Failed to search messages",
                    )
                }
            }
        }
    }
}