package com.ekhonavigator.feature.canvas.settings

const val DEFAULT_CANVAS_DOMAIN = "csuci.instructure.com"

sealed interface ConnectCanvasUiState {

    data object Loading : ConnectCanvasUiState

    data class NotConnected(
        val domain: String = DEFAULT_CANVAS_DOMAIN,
        val token: String = "",
        val isConnecting: Boolean = false,
        val error: String? = null,
    ) : ConnectCanvasUiState

    data class Connected(
        val domain: String,
    ) : ConnectCanvasUiState
}
