package com.ekhonavigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.designsystem.theme.LocalSignedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Provides [LocalSignedIn] into composition. Wraps the auth state so any
 * descendant can gate sign-in-required affordances (create-event FAB, future
 * profile-edit entry points, etc.) behind a single read.
 */
@Composable
fun SignedInProvider(
    viewModel: SignedInViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val signedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalSignedIn provides signedIn) {
        content()
    }
}

@HiltViewModel
class SignedInViewModel @Inject constructor(
    @Suppress("unused") savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = authRepository.userFlow()
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = authRepository.getCurrentUserUid() != null,
        )
}
