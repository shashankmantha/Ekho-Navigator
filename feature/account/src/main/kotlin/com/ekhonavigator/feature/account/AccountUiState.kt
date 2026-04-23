package com.ekhonavigator.feature.account

import com.ekhonavigator.core.model.OnlineStatus

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data object SignedOut : AccountUiState

    data class SignedIn(
        val email: String,
        val displayName: String,
        val major: String,
        val description: String,
        val links: String,
        val majorVisible: Boolean,
        val descriptionVisible: Boolean,
        val linksVisible: Boolean,
        val avatarId: String,
        val searchable: Boolean,
        val showOnlineStatus: Boolean,
        val onlineStatus: OnlineStatus,
    ) : AccountUiState

    data class Error(val message: String) : AccountUiState
}
