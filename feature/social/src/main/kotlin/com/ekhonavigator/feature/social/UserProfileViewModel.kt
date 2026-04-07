package com.ekhonavigator.feature.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.social.SocialRepository
import com.ekhonavigator.core.data.social.SocialUser
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
    val errorMessage: String? = null,
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: SocialRepository,
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

            try {
                val user = repository.getUserById(userId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        errorMessage = if (user == null) "User not found" else null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = null,
                        errorMessage = e.message ?: "Failed to load profile",
                    )
                }
            }
        }
    }
}