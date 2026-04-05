package com.ekhonavigator.core.data.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUserEmail(): String?

    fun getCurrentUserDisplayName(): String?

    fun getCurrentUserUid(): String?

    fun userFlow(): Flow<String?>

    suspend fun signInWithGoogle(
        context: Context,
        webClientId: String,
    )

    fun signOut()
}