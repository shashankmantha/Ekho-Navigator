package com.ekhonavigator.core.canvas.auth

/** Per-(user, institution) key for a stored Canvas PAT. */
data class CanvasAccount(
    val firebaseUid: String,
    val domain: String,
)
