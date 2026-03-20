package com.ekhonavigator.core.data.auth

import android.content.Context

interface AuthRepository {
    fun getCurrentUserEmail(): String?

    fun getCurrentUserDisplayName(): String?

    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String,
    )

    fun signOut()
}