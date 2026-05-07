package com.ekhonavigator.core.canvas.auth

/**
 * Boundary interface so core:canvas stays free of Firebase dependencies.
 * core:data binds the Firebase-backed implementation.
 */
interface CanvasIdentitySource {
    fun currentUid(): String?
}
