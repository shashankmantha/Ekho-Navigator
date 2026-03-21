package com.ekhonavigator.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.auth.FirebaseAuthRepository
import com.ekhonavigator.core.data.profile.FirestoreProfileRepository
import com.ekhonavigator.core.data.profile.ProfileRepository
import com.ekhonavigator.core.data.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authRepo: AuthRepository = FirebaseAuthRepository(),
    private val profileRepo: ProfileRepository = FirestoreProfileRepository(),
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
                    displayName = profile?.displayName?.ifBlank { displayName ?: "" } ?: (displayName ?: ""),
                    major = profile?.major ?: "",
                    description = profile?.description ?: "",
                    links = profile?.links ?: "",
                    majorVisible = profile?.majorVisible ?: true,
                    descriptionVisible = profile?.descriptionVisible ?: true,
                    linksVisible = profile?.linksVisible ?: true,
                    avatarId = profile?.avatarId ?: "avatar_default",
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
    ) {
        val uid = authRepo.getCurrentUserUid() ?: return
        val email = authRepo.getCurrentUserEmail() ?: ""

        viewModelScope.launch {
            try {
                profileRepo.saveProfile(
                    uid = uid,
                    profile = UserProfile(
                        displayName = displayName,
                        email = email,
                        major = major,
                        description = description,
                        links = links,
                        majorVisible = majorVisible,
                        descriptionVisible = descriptionVisible,
                        linksVisible = linksVisible,
                        avatarId = avatarId,
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
        authRepo.signOut()
        checkUser()
    }
}