package com.ekhonavigator.feature.canvas.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.canvas.auth.CanvasAuthError
import com.ekhonavigator.core.canvas.auth.CanvasAuthValidator
import com.ekhonavigator.core.canvas.auth.CanvasIdentitySource
import com.ekhonavigator.core.canvas.auth.CanvasInstitutionStore
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ConnectCanvasViewModel @Inject constructor(
    private val identitySource: CanvasIdentitySource,
    private val institutionStore: CanvasInstitutionStore,
    private val tokenStore: CanvasTokenStore,
    private val validator: CanvasAuthValidator,
    private val canvasCourseRepository: CanvasCourseRepository,
    private val canvasPlannerRepository: CanvasPlannerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConnectCanvasUiState>(ConnectCanvasUiState.Loading)
    val uiState: StateFlow<ConnectCanvasUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = resolveInitialState()
    }

    fun setDomain(domain: String) {
        val current = _uiState.value as? ConnectCanvasUiState.NotConnected ?: return
        _uiState.value = current.copy(domain = domain.trim(), error = null)
    }

    fun setToken(token: String) {
        val current = _uiState.value as? ConnectCanvasUiState.NotConnected ?: return
        // Strip ALL whitespace, not just edges — Canvas tokens are alphanumeric,
        // and copy-paste sometimes injects a hard wrap mid-string. OkHttp's
        // Authorization header validator rejects any \n with
        // IllegalArgumentException at the Retrofit call site.
        _uiState.value = current.copy(token = token.stripAllWhitespace(), error = null)
    }

    fun connect() {
        val current = _uiState.value as? ConnectCanvasUiState.NotConnected ?: return
        val sanitizedDomain = current.domain.trim()
        val sanitizedToken = current.token.stripAllWhitespace()
        if (sanitizedDomain.isBlank() || sanitizedToken.isBlank() || current.isConnecting) return

        val uid = identitySource.currentUid()
        if (uid == null) {
            _uiState.value = current.copy(error = "You must be signed in to connect Canvas.")
            return
        }

        _uiState.value = current.copy(isConnecting = true, error = null)
        viewModelScope.launch {
            validator.validate(domain = sanitizedDomain, token = sanitizedToken).fold(
                onSuccess = {
                    val account = CanvasAccount(firebaseUid = uid, domain = sanitizedDomain)
                    institutionStore.setDomain(uid, sanitizedDomain)
                    tokenStore.put(account, sanitizedToken)
                    _uiState.value = ConnectCanvasUiState.Connected(domain = sanitizedDomain)
                    // Kick an immediate sync so the user sees content as soon as they
                    // navigate back. Otherwise courses + planner items only appear after
                    // they happen to open the Calendar tab (the only other sync trigger).
                    triggerInitialSync()
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

    private suspend fun triggerInitialSync() {
        canvasCourseRepository.sync()
        val zone = ZoneId.systemDefault()
        val now = YearMonth.now()
        val start = now.minusMonths(2).atDay(1).atStartOfDay(zone).toInstant()
        val end = now.plusMonths(3).atDay(1).atStartOfDay(zone).toInstant()
        canvasPlannerRepository.sync(start, end)
    }

    fun disconnect() {
        val connected = _uiState.value as? ConnectCanvasUiState.Connected ?: return
        val uid = identitySource.currentUid()
        if (uid != null) {
            tokenStore.delete(CanvasAccount(firebaseUid = uid, domain = connected.domain))
            institutionStore.clearDomain(uid)
        }
        // Wipe Canvas data alongside the credentials. Without this, courses +
        // bridged Canvas event pills linger on Calendar/Discover until the
        // next sign-out — confusing for a "I just disconnected" user.
        viewModelScope.launch {
            runCatching { canvasCourseRepository.clearAll() }
            runCatching { canvasPlannerRepository.clearAll() }
        }
        _uiState.value = ConnectCanvasUiState.NotConnected()
    }

    private fun resolveInitialState(): ConnectCanvasUiState {
        val uid = identitySource.currentUid() ?: return ConnectCanvasUiState.NotConnected()
        val domain = institutionStore.getDomain(uid) ?: return ConnectCanvasUiState.NotConnected()
        // Don't claim Connected if the PAT is missing — institution + token can drift
        // out of sync (e.g. legacy state from before sign-out cleanup was scoped right).
        // Without a token, every API call 401s, so the UI must let the user re-enter.
        val account = CanvasAccount(firebaseUid = uid, domain = domain)
        if (tokenStore.get(account).isNullOrBlank()) {
            institutionStore.clearDomain(uid)
            return ConnectCanvasUiState.NotConnected(domain = domain)
        }
        return ConnectCanvasUiState.Connected(domain = domain)
    }

    private fun String.stripAllWhitespace(): String =
        filterNot { it.isWhitespace() }

    private fun Throwable.toUserMessage(): String = when (this) {
        is CanvasAuthError.InvalidDomain -> "That doesn't look like a Canvas domain."
        is CanvasAuthError.InvalidToken -> "Canvas didn't accept that token. Double-check and try again."
        is CanvasAuthError.HttpError -> "Canvas returned an error (HTTP $code). Try again later."
        is CanvasAuthError.Network -> "Couldn't reach Canvas. Check your connection."
        is CanvasAuthError.ParseError -> "Got an unexpected response from Canvas."
        else -> message ?: "Something went wrong."
    }
}
