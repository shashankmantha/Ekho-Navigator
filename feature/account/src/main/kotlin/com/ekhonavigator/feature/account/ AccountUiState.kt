package com.ekhonavigator.feature.account

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data object SignedOut : AccountUiState
    data class SignedIn(val email: String?) : AccountUiState
    data class Error(val message: String) : AccountUiState
}