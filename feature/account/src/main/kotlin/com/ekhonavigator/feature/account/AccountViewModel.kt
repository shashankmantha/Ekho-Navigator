package com.ekhonavigator.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repo: AuthRepository = FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        checkUser()
    }

    fun checkUser() {
        val email = repo.getCurrentUserEmail()
        _uiState.value = if (email == null) {
            AccountUiState.SignedOut
        } else {
            AccountUiState.SignedIn(email)
        }
    }
    fun onGoogleSignInClick(
        context: Context,
        clientId: String
    ) {
        viewModelScope.launch {
            _uiState.value = AccountUiState.Loading
            try {
                repo.signInWithGoogle(context, clientId)
                checkUser()
            } catch (e: Exception) {
                _uiState.value = AccountUiState.Error(
                    e.message ?: "Sign-in failed"
                )
            }
        }
    }

    fun onSignOutClick() {
        repo.signOut()
        checkUser()
    }
}