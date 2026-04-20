package com.ekhonavigator.core.data.social

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

open class SocialRepository @Inject constructor() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun getUserById(userId: String): SocialUser? {
        val doc = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        if (!doc.exists()) return null

        val displayName = doc.getString("displayName") ?: return null

        return SocialUser(
            id = doc.id,
            displayName = displayName,
            email = doc.getString("email") ?: "",
            major = doc.getString("major") ?: "",
            description = doc.getString("description") ?: "",
            links = doc.getString("links") ?: "",
            avatarId = doc.getString("avatarId") ?: "avatar_default",
            majorVisible = doc.getBoolean("majorVisible") ?: false,
            descriptionVisible = doc.getBoolean("descriptionVisible") ?: false,
            linksVisible = doc.getBoolean("linksVisible") ?: false,
            showOnlineStatus = doc.getBoolean("showOnlineStatus") ?: true,
        )
    }

    suspend fun getOutgoingRequestIds(currentUserId: String): Set<String> {
        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .collection("outgoingRequests")
            .get()
            .await()

        return snapshot.documents.map { it.id }.toSet()
    }

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

        Log.d(
            "SocialSearch",
            "docs=${
                snapshot.documents.map {
                    "${it.id}:${it.getString("displayNameLower")}:${it.getBoolean("searchable")}"
                }
            }"
        )

        return snapshot.documents.mapNotNull { doc ->
            val displayName = doc.getString("displayName") ?: return@mapNotNull null
            val searchable = doc.getBoolean("searchable") ?: false

            if (!searchable) return@mapNotNull null

            SocialUser(
                id = doc.id,
                displayName = displayName,
                email = doc.getString("email") ?: "",
                major = doc.getString("major") ?: "",
                description = doc.getString("description") ?: "",
                links = doc.getString("links") ?: "",
                avatarId = doc.getString("avatarId") ?: "avatar_default",
                majorVisible = doc.getBoolean("majorVisible") ?: false,
                descriptionVisible = doc.getBoolean("descriptionVisible") ?: false,
                linksVisible = doc.getBoolean("linksVisible") ?: false,
                showOnlineStatus = doc.getBoolean("showOnlineStatus") ?: true,
            )
        }.filter { it.id != currentUserId }
    }

    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return

        val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
        val targetUserDoc = firestore.collection("users").document(targetUserId).get().await()

        if (!currentUserDoc.exists() || !targetUserDoc.exists()) return

        val currentDisplayName = currentUserDoc.getString("displayName") ?: ""
        val currentAvatarId = currentUserDoc.getString("avatarId") ?: "avatar_default"
        val currentMajor = currentUserDoc.getString("major") ?: ""

        val targetDisplayName = targetUserDoc.getString("displayName") ?: ""
        val targetAvatarId = targetUserDoc.getString("avatarId") ?: "avatar_default"
        val targetMajor = targetUserDoc.getString("major") ?: ""

        val incomingRequestData = mapOf(
            "uid" to currentUserId,
            "displayName" to currentDisplayName,
            "avatarId" to currentAvatarId,
            "major" to currentMajor,
        )

        val outgoingRequestData = mapOf(
            "uid" to targetUserId,
            "displayName" to targetDisplayName,
            "avatarId" to targetAvatarId,
            "major" to targetMajor,
        )

        val batch = firestore.batch()

        val incomingRef = firestore.collection("users")
            .document(targetUserId)
            .collection("incomingRequests")
            .document(currentUserId)

        val outgoingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("outgoingRequests")
            .document(targetUserId)

        batch.set(incomingRef, incomingRequestData)
        batch.set(outgoingRef, outgoingRequestData)
        batch.commit().await()
    }

    suspend fun getIncomingRequests(currentUserId: String): List<FriendRequest> {
        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .collection("incomingRequests")
            .get()
            .await()

        return snapshot.documents.map { it.toFriendRequest() }
    }

    open fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .collection("incomingRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.map { it.toFriendRequest() } ?: emptyList()
                trySend(requests)
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toFriendRequest(): FriendRequest = FriendRequest(
        uid = getString("uid") ?: "",
        displayName = getString("displayName") ?: "",
        avatarId = getString("avatarId") ?: "avatar_default",
        major = getString("major") ?: "",
    )

    suspend fun getFriends(currentUserId: String): List<FriendUser> {
        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .get()
            .await()

        return snapshot.documents.mapNotNull {
            val uid = it.getString("uid") ?: return@mapNotNull null
            val displayName = it.getString("displayName") ?: return@mapNotNull null

            if (uid == currentUserId) return@mapNotNull null

            FriendUser(
                uid = uid,
                displayName = displayName,
                avatarId = it.getString("avatarId") ?: "avatar_default",
                major = it.getString("major") ?: "",
                showOnlineStatus = it.getBoolean("showOnlineStatus") ?: true,
            )
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, fromUserId: String) {
        val currentUserDoc = firestore.collection("users").document(currentUserId).get().await()
        val fromUserDoc = firestore.collection("users").document(fromUserId).get().await()

        if (!currentUserDoc.exists() || !fromUserDoc.exists()) return

        val currentUserData = mapOf(
            "uid" to currentUserId,
            "displayName" to (currentUserDoc.getString("displayName") ?: ""),
            "avatarId" to (currentUserDoc.getString("avatarId") ?: "avatar_default"),
            "major" to (currentUserDoc.getString("major") ?: ""),
            "showOnlineStatus" to (currentUserDoc.getBoolean("showOnlineStatus") ?: true),
        )

        val fromUserData = mapOf(
            "uid" to fromUserId,
            "displayName" to (fromUserDoc.getString("displayName") ?: ""),
            "avatarId" to (fromUserDoc.getString("avatarId") ?: "avatar_default"),
            "major" to (fromUserDoc.getString("major") ?: ""),
            "showOnlineStatus" to (fromUserDoc.getBoolean("showOnlineStatus") ?: true),
        )

        val batch = firestore.batch()

        val incomingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("incomingRequests")
            .document(fromUserId)

        val outgoingRef = firestore.collection("users")
            .document(fromUserId)
            .collection("outgoingRequests")
            .document(currentUserId)

        val reverseIncomingRef = firestore.collection("users")
            .document(fromUserId)
            .collection("incomingRequests")
            .document(currentUserId)

        val reverseOutgoingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("outgoingRequests")
            .document(fromUserId)

        val currentFriendRef = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .document(fromUserId)

        val fromFriendRef = firestore.collection("users")
            .document(fromUserId)
            .collection("friends")
            .document(currentUserId)

        batch.delete(incomingRef)
        batch.delete(outgoingRef)
        batch.delete(reverseIncomingRef)
        batch.delete(reverseOutgoingRef)

        batch.set(currentFriendRef, fromUserData)
        batch.set(fromFriendRef, currentUserData)

        batch.commit().await()
    }

    suspend fun denyFriendRequest(currentUserId: String, fromUserId: String) {
        val batch = firestore.batch()

        val incomingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("incomingRequests")
            .document(fromUserId)

        val outgoingRef = firestore.collection("users")
            .document(fromUserId)
            .collection("outgoingRequests")
            .document(currentUserId)

        batch.delete(incomingRef)
        batch.delete(outgoingRef)
        batch.commit().await()
    }

    suspend fun removeFriend(currentUserId: String, friendUserId: String) {
        if (currentUserId == friendUserId) return

        val batch = firestore.batch()

        val currentFriendRef = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .document(friendUserId)

        val friendSideRef = firestore.collection("users")
            .document(friendUserId)
            .collection("friends")
            .document(currentUserId)

        batch.delete(currentFriendRef)
        batch.delete(friendSideRef)
        batch.commit().await()
    }
}
