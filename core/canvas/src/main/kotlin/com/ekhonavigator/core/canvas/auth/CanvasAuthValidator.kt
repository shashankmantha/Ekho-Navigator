package com.ekhonavigator.core.canvas.auth

import com.ekhonavigator.core.canvas.model.CanvasProfile

interface CanvasAuthValidator {
    suspend fun validate(domain: String, token: String): Result<CanvasProfile>
}

sealed class CanvasAuthError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    object InvalidDomain : CanvasAuthError("Invalid Canvas domain")
    object InvalidToken : CanvasAuthError("Token rejected by Canvas")
    class HttpError(val code: Int) : CanvasAuthError("Canvas returned HTTP $code")
    class Network(cause: Throwable) : CanvasAuthError("Network error: ${cause.message}", cause)
    class ParseError(cause: Throwable) : CanvasAuthError("Failed to parse Canvas response", cause)
}
