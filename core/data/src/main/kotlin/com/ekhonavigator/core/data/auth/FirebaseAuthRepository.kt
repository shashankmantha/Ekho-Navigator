package com.ekhonavigator.core.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor() : AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    override fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    override fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    override fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    override fun userFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.uid)
        }

        auth.addAuthStateListener(listener)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signInWithGoogle(
        context: Context,
        webClientId: String,
    ) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = context,
            request = request,
        )

        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(credential.data)

                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken,
                    null,
                )

                auth.signInWithCredential(firebaseCredential).await()

                saveFcmTokenForCurrentUser()
            } catch (e: GoogleIdTokenParsingException) {
                throw Exception("Google ID token parsing failed", e)
            }
        } else {
            throw Exception("Unexpected credential type")
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    private fun saveFcmTokenForCurrentUser() {
        val uid = auth.currentUser?.uid

        android.util.Log.d("FCM_TOKEN", "Current uid: $uid")

        if (uid == null) return

        firebaseMessaging.token
            .addOnSuccessListener { token ->
                android.util.Log.d("FCM_TOKEN", "Token received: $token")

                val tokenData = mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to System.currentTimeMillis(),
                )

                firestore.collection("users")
                    .document(uid)
                    .collection("fcmTokens")
                    .document(token)
                    .set(tokenData)
                    .addOnSuccessListener {
                        android.util.Log.d("FCM_TOKEN", "Token saved successfully")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FCM_TOKEN", "Failed to save token", e)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FCM_TOKEN", "Failed to get token", e)
            }
    }
}