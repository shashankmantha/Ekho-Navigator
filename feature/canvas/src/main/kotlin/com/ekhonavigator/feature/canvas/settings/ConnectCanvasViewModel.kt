package com.ekhonavigator.feature.canvas.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.canvas.auth.CanvasAuthError
import com.ekhonavigator.core.canvas.auth.CanvasAuthValidator
import com.ekhonavigator.core.canvas.auth.CanvasIdentitySource
import com.ekhonavigator.core.canvas.auth.CanvasInstitutionStore
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectCanvasViewModel @Inject constructor(
    private val identitySource: CanvasIdentitySource,
    private val institutionStore: CanvasInstitutionStore,
    private val tokenStore: CanvasTokenStore,
    private val validator: CanvasAuthValidator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectCanvasUiState>(ConnectCanvasUiState.Loading)
    val uiState: StateFlow<ConnectCanvasUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = resolveInitialState()
    }

    fun setDomain(domain: String) {
        val current = _uiState.value as? ConnectCanvasUiState.NotConnected ?: return
        _uiState.value = current.copy(domain = domain, error = null)
    }

    fun setToken(token: String) {
        val current = _uiState.value as? ConnectCanvasUiState.NotConnected ?: return
        _uiState.value = current.copy(token = token, error = null)
    }

    fun connect() {
        val current = _uiState.value as? ConnectCanvasUiState.NotConnected ?: return
        if (current.domain.isBlank() || current.token.isBlank() || current.isConnecting) return

        val uid = identitySource.currentUid()
        if (uid == null) {
            _uiState.value = current.copy(error = "You must be signed in to connect Canvas.")
            return
        }

        _uiState.value = current.copy(isConnecting = true, error = null)
        viewModelScope.launch {
            validator.validate(domain = current.domain, token = current.token).fold(
                onSuccess = {
                    val account = CanvasAccount(firebaseUid = uid, domain = current.domain)
                    institutionStore.setDomain(uid, current.domain)
                    tokenStore.put(account, current.token)
                    _uiState.value = ConnectCanvasUiState.Connected(domain = current.domain)
                },
                onFailure = { error ->
                    _uiState.value = current.copy(
                        isConnecting = false,
                        error = error.toUserMessage(),
                    )
                },
            )
        }
    }

    fun disconnect() {
        val connected = _uiState.value as? ConnectCanvasUiState.Connected ?: return
        val uid = identitySource.currentUid()
        if (uid != null) {
            tokenStore.delete(CanvasAccount(firebaseUid = uid, domain = connected.domain))
            institutionStore.clearDomain(uid)
        }
        _uiState.value = ConnectCanvasUiState.NotConnected()
    }

    private fun resolveInitialState(): ConnectCanvasUiState {
        val uid = identitySource.currentUid() ?: return ConnectCanvasUiState.NotConnected()
        val domain = institutionStore.getDomain(uid) ?: return ConnectCanvasUiState.NotConnected()
        return ConnectCanvasUiState.Connected(domain = domain)
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is CanvasAuthError.InvalidDomain -> "That doesn't look like a Canvas domain."
        is CanvasAuthError.InvalidToken -> "Canvas didn't accept that token. Double-check and try again."
        is CanvasAuthError.HttpError -> "Canvas returned an error (HTTP $code). Try again later."
        is CanvasAuthError.Network -> "Couldn't reach Canvas. Check your connection."
        is CanvasAuthError.ParseError -> "Got an unexpected response from Canvas."
        else -> message ?: "Something went wrong."
    }
}
