package com.ekhonavigator.core.testing

import android.content.Context
import com.ekhonavigator.core.data.auth.AuthRepository

/**
 * Fake [AuthRepository] for unit tests.
 *
 * Returns a fixed test user by default. Set [uid] to null to simulate
 * a signed-out state.
 */
class TestAuthRepository(
    var uid: String? = "test-user-uid",
    var email: String? = "test@example.com",
    var displayName: String? = "Test User",
) : AuthRepository {

    override fun getCurrentUserUid(): String? = uid

    override fun getCurrentUserEmail(): String? = email

    override fun getCurrentUserDisplayName(): String? = displayName

    override suspend fun signInWithGoogle(context: Context, webClientId: String) {
        uid = "test-user-uid"
    }

    override fun signOut() {
        uid = null
    }
}
