package com.ekhonavigator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.data.canvas.CanvasConnectionState
import com.ekhonavigator.core.designsystem.theme.LocalCanvasConnected
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Composable
fun CanvasConnectionProvider(
    viewModel: CanvasConnectionViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val connected by viewModel.isConnected.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalCanvasConnected provides connected) {
        content()
    }
}

@HiltViewModel
class CanvasConnectionViewModel @Inject constructor(
    @Suppress("unused") savedStateHandle: SavedStateHandle,
    state: CanvasConnectionState,
) : ViewModel() {

    val isConnected: StateFlow<Boolean> = state.isConnected.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )
}
