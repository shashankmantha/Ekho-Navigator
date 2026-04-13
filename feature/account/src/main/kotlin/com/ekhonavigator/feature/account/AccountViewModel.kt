package com.ekhonavigator.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.profile.ProfileRepository
import com.ekhonavigator.core.data.profile.UserProfile
import com.ekhonavigator.core.data.repository.PresenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository,
    private val presenceRepo: PresenceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        checkUser()
    }

    fun checkUser() {
        val uid = authRepo.getCurrentUserUid()
        val email = authRepo.getCurrentUserEmail()
        val displayName = authRepo.getCurrentUserDisplayName()

        if (uid == null || email == null) {
            _uiState.value = AccountUiState.SignedOut
            return
        }

        viewModelScope.launch {
            try {
                val profile = profileRepo.getProfile(uid)

                _uiState.value = AccountUiState.SignedIn(
                    email = email,
                    displayName = profile?.displayName?.ifBlank { displayName ?: "" }
                        ?: (displayName ?: ""),
                    major = profile?.major ?: "",
                    description = profile?.description ?: "",
                    links = profile?.links ?: "",
                    majorVisible = profile?.majorVisible ?: true,
                    descriptionVisible = profile?.descriptionVisible ?: true,
                    linksVisible = profile?.linksVisible ?: true,
                    avatarId = profile?.avatarId ?: "avatar_default",
                    searchable = profile?.searchable ?: true,
                    showOnlineStatus = profile?.showOnlineStatus ?: true,
                )
            } catch (e: Exception) {
                _uiState.value = AccountUiState.Error(
                    e.message ?: "Failed to load account information"
                )
            }
        }
    }

    fun onGoogleSignInClick(
        context: Context,
        clientId: String,
    ) {
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            try {
                authRepo.signInWithGoogle(context, clientId)
                checkUser()
            } catch (e: Exception) {
                _uiState.value = AccountUiState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun saveProfile(
        displayName: String,
        major: String,
        description: String,
        links: String,
        majorVisible: Boolean,
        descriptionVisible: Boolean,
        linksVisible: Boolean,
        avatarId: String,
        searchable: Boolean,
        showOnlineStatus: Boolean,
    ) {
        val uid = authRepo.getCurrentUserUid() ?: return
        val email = authRepo.getCurrentUserEmail() ?: ""

        viewModelScope.launch {
            try {
                profileRepo.saveProfile(
                    uid = uid,
                    profile = UserProfile(
                        displayName = displayName,
                        displayNameLower = displayName.trim().lowercase(),
                        email = email,
                        emailLower = email.trim().lowercase(),
                        major = major,
                        majorLower = major.trim().lowercase(),
                        description = description,
                        links = links,
                        majorVisible = majorVisible,
                        descriptionVisible = descriptionVisible,
                        linksVisible = linksVisible,
                        avatarId = avatarId,
                        searchable = searchable,
                        showOnlineStatus = showOnlineStatus,
                    ),
                )
                checkUser()
            } catch (e: Exception) {
                _uiState.value = AccountUiState.Error(
                    e.message ?: "Failed to save profile"
                )
            }
        }
    }

    fun onSignOutClick() {
        val uid = authRepo.getCurrentUserUid()

        viewModelScope.launch {
            try {
                // 1. Write offline status BEFORE clearing auth
                if (uid != null) {
                    presenceRepo.setOfflineNow(uid)
                }
            } catch (_: Exception) {
                // Ignore errors during offline write
            } finally {
                // 2. Stop tracking
                presenceRepo.stopPresence()
                // 3. Clear auth
                authRepo.signOut()
                // 4. Update UI
                _uiState.value = AccountUiState.SignedOut
            }
        }
    }
}
