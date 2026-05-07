package com.ekhonavigator.core.data.social

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

open class SocialRepository @Inject constructor() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        private const val TAG = "SocialRepository"
    }

    suspend fun getUserById(userId: String): SocialUser? {
        if (userId.isBlank()) return null

        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            doc.toSocialUserOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "getUserById failed", e)
            null
        }
    }

    suspend fun getOutgoingRequestIds(currentUserId: String): Set<String> {
        if (currentUserId.isBlank()) return emptySet()

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("outgoingRequests")
                .get()
                .await()

            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "getOutgoingRequestIds failed", e)
            emptySet()
        }
    }

    suspend fun searchUsersByName(
        query: String,
        currentUserId: String? = null,
    ): List<SocialUser> {
        val trimmed = query.trim().lowercase()

        if (trimmed.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("users")
                .orderBy("displayNameLower")
                .whereGreaterThanOrEqualTo("displayNameLower", trimmed)
                .whereLessThanOrEqualTo("displayNameLower", trimmed + "\uf8ff")
                .limit(20)
                .get()
                .await()

            Log.d(
                TAG,
                "searchUsersByName docs=${
                    snapshot.documents.map {
                        "${it.id}:${it.getString("displayNameLower")}:${it.getBoolean("searchable")}"
                    }
                }"
            )

            snapshot.documents.mapNotNull { doc ->
                val searchable = doc.getBoolean("searchable") ?: false
                if (!searchable) return@mapNotNull null

                val user = doc.toSocialUserOrNull() ?: return@mapNotNull null

                if (user.id == currentUserId) return@mapNotNull null

                user
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchUsersByName failed", e)
            emptyList()
        }
    }

    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String) {
        if (currentUserId.isBlank() || targetUserId.isBlank()) return
        if (currentUserId == targetUserId) return

        try {
            val currentUserDoc = firestore.collection("users")
                .document(currentUserId)
                .get()
                .await()

            val targetUserDoc = firestore.collection("users")
                .document(targetUserId)
                .get()
                .await()

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
        } catch (e: Exception) {
            Log.e(TAG, "sendFriendRequest failed", e)
        }
    }

    suspend fun getIncomingRequests(currentUserId: String): List<FriendRequest> {
        if (currentUserId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("incomingRequests")
                .get()
                .await()

            snapshot.documents.map { it.toFriendRequest() }
        } catch (e: Exception) {
            Log.e(TAG, "getIncomingRequests failed", e)
            emptyList()
        }
    }

    open fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users")
            .document(userId)
            .collection("incomingRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeIncomingRequests failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents
                    ?.map { it.toFriendRequest() }
                    ?: emptyList()

                trySend(requests)
            }

        awaitClose {
            registration.remove()
        }
    }

    open fun observeUser(userId: String): Flow<SocialUser?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e(TAG, "observeUser failed", error)
                    trySend(null)
                    return@addSnapshotListener
                }

                val user = doc?.toSocialUserOrNull()
                trySend(user)
            }

        awaitClose {
            registration.remove()
        }
    }

    open fun observeFriends(currentUserId: String): Flow<List<FriendUser>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeFriends failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toFriendUserOrNull(currentUserId)
                } ?: emptyList()

                trySend(friendList)
            }

        awaitClose {
            registration.remove()
        }
    }

    open fun observeIsFriend(
        currentUserId: String,
        targetUserId: String,
    ): Flow<Boolean> = callbackFlow {
        if (currentUserId.isBlank() || targetUserId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .document(targetUserId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    Log.e(TAG, "observeIsFriend failed", error)
                    trySend(false)
                    return@addSnapshotListener
                }

                trySend(doc?.exists() == true)
            }

        awaitClose {
            registration.remove()
        }
    }

    open suspend fun getFriends(currentUserId: String): List<FriendUser> {
        if (currentUserId.isBlank()) return emptyList()

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toFriendUserOrNull(currentUserId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFriends failed", e)
            emptyList()
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, fromUserId: String) {
        if (currentUserId.isBlank() || fromUserId.isBlank()) return
        if (currentUserId == fromUserId) return

        try {
            val currentUserDoc = firestore.collection("users")
                .document(currentUserId)
                .get()
                .await()

            val fromUserDoc = firestore.collection("users")
                .document(fromUserId)
                .get()
                .await()

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
        } catch (e: Exception) {
            Log.e(TAG, "acceptFriendRequest failed", e)
        }
    }

    suspend fun denyFriendRequest(currentUserId: String, fromUserId: String) {
        if (currentUserId.isBlank() || fromUserId.isBlank()) return

        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "denyFriendRequest failed", e)
        }
    }

    suspend fun removeFriend(currentUserId: String, friendUserId: String) {
        if (currentUserId.isBlank() || friendUserId.isBlank()) return
        if (currentUserId == friendUserId) return

        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "removeFriend failed", e)
        }
    }

    private fun DocumentSnapshot.toSocialUserOrNull(): SocialUser? {
        if (!exists()) return null

        val displayName = getString("displayName") ?: return null

        return SocialUser(
            id = id,
            displayName = displayName,
            email = getString("email") ?: "",
            major = getString("major") ?: "",
            description = getString("description") ?: "",
            links = getString("links") ?: "",
            avatarId = getString("avatarId") ?: "avatar_default",
            majorVisible = getBoolean("majorVisible") ?: false,
            descriptionVisible = getBoolean("descriptionVisible") ?: false,
            linksVisible = getBoolean("linksVisible") ?: false,
            showOnlineStatus = getBoolean("showOnlineStatus") ?: true,
        )
    }

    private fun DocumentSnapshot.toFriendRequest(): FriendRequest = FriendRequest(
        uid = getString("uid") ?: "",
        displayName = getString("displayName") ?: "",
        avatarId = getString("avatarId") ?: "avatar_default",
        major = getString("major") ?: "",
    )

    private fun DocumentSnapshot.toFriendUserOrNull(currentUserId: String): FriendUser? {
        val uid = getString("uid") ?: return null
        val displayName = getString("displayName") ?: return null

        if (uid == currentUserId) return null

        return FriendUser(
            uid = uid,
            displayName = displayName,
            avatarId = getString("avatarId") ?: "avatar_default",
            major = getString("major") ?: "",
            showOnlineStatus = getBoolean("showOnlineStatus") ?: true,
        )
    }
}