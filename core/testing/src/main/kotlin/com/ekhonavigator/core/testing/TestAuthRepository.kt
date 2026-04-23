package com.ekhonavigator.core.testing

import android.content.Context
import com.ekhonavigator.core.data.auth.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestAuthRepository(
    var uid: String? = "test-user-uid",
    var email: String? = "test@example.com",
    var displayName: String? = "Test User",
) : AuthRepository {

    private val _userFlow = MutableStateFlow(uid)

    override fun getCurrentUserUid(): String? = uid

    override fun getCurrentUserEmail(): String? = email

    override fun getCurrentUserDisplayName(): String? = displayName

    override fun getCurrentUser(): FirebaseUser? = null

    override fun userFlow(): Flow<String?> = _userFlow

    override suspend fun signInWithGoogle(context: Context, webClientId: String) {
        uid = "test-user-uid"
        _userFlow.value = uid
    }

    override fun signOut() {
        uid = null
        _userFlow.value = null
    }
}
