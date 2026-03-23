package com.ekhonavigator.core.data.social

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

data class SocialUser(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val major: String = "",
    val description: String = "",
    val avatarId: String = "avatar_default",
)

class SocialRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun searchUsersByName(
        query: String,
        currentUserId: String? = null,
    ): List<SocialUser> {
        val trimmed = query.trim().lowercase()

        if (trimmed.isBlank()) return emptyList()

        val snapshot = firestore.collection("users")
            .orderBy("displayNameLower")
            .whereGreaterThanOrEqualTo("displayNameLower", trimmed)
            .whereLessThanOrEqualTo("displayNameLower", trimmed + "\uf8ff")
            .limit(20)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val displayName = doc.getString("displayName") ?: return@mapNotNull null

            SocialUser(
                id = doc.id,
                displayName = displayName,
                email = doc.getString("email") ?: "",
                major = doc.getString("major") ?: "",
                description = doc.getString("description") ?: "",
                avatarId = doc.getString("avatarId") ?: "avatar_default",
            )
        }.filter { it.id != currentUserId }
    }
}