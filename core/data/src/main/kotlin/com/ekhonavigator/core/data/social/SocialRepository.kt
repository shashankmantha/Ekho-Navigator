package com.ekhonavigator.core.data.social

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

open class SocialRepository @Inject constructor() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun observeUser(userId: String): Flow<SocialUser?> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                trySend(snapshot.toSocialUser())
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toSocialUser(): SocialUser? {
        if (!exists()) return null
        return SocialUser(
            id = id,
            displayName = getString("displayName") ?: "",
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

    suspend fun getUserById(userId: String): SocialUser? {
        val doc = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        return doc.toSocialUser()
    }

    suspend fun getOutgoingRequestIds(currentUserId: String): Set<String> {
        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .collection("outgoingRequests")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.getString("uid") }.toSet()
    }

    suspend fun searchUsersByName(query: String, currentUserId: String?): List<SocialUser> {
        val lowercaseQuery = query.lowercase()
        val snapshot = firestore.collection("users")
            .whereGreaterThanOrEqualTo("displayNameLower", lowercaseQuery)
            .whereLessThanOrEqualTo("displayNameLower", lowercaseQuery + "\uf8ff")
            .limit(20)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val searchable = doc.getBoolean("searchable") ?: false
            if (!searchable) return@mapNotNull null

            doc.toSocialUser()
        }.filter { it.id != currentUserId }
    }

    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return

        val batch = firestore.batch()

        val incomingRef = firestore.collection("users")
            .document(targetUserId)
            .collection("incomingRequests")
            .document(currentUserId)

        val outgoingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("outgoingRequests")
            .document(targetUserId)

        batch.set(incomingRef, mapOf("uid" to currentUserId, "timestamp" to System.currentTimeMillis()))
        batch.set(outgoingRef, mapOf("uid" to targetUserId, "timestamp" to System.currentTimeMillis()))
        batch.commit().await()
    }

    suspend fun getIncomingRequests(currentUserId: String): List<FriendRequest> {
        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .collection("incomingRequests")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val uid = doc.getString("uid") ?: return@mapNotNull null
            val user = getUserById(uid) ?: return@mapNotNull null
            FriendRequest(
                uid = user.id,
                displayName = user.displayName,
                avatarId = user.avatarId,
                major = user.major
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    open fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> {
        return observeIncomingRequestUids(userId).flatMapLatest { uids ->
            if (uids.isEmpty()) return@flatMapLatest flowOf(emptyList())
            
            val requestFlows = uids.map { uid ->
                observeUser(uid).map { user ->
                    user?.let {
                        FriendRequest(
                            uid = it.id,
                            displayName = it.displayName,
                            avatarId = it.avatarId,
                            major = it.major
                        )
                    }
                }
            }
            
            combine(requestFlows) { requests ->
                requests.filterNotNull()
            }
        }
    }

    private fun observeIncomingRequestUids(userId: String): Flow<List<String>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .collection("incomingRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val uids = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("uid")
                } ?: emptyList()
                trySend(uids)
            }
        awaitClose { registration.remove() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    open fun observeFriends(currentUserId: String): Flow<List<FriendUser>> {
        return observeFriendUids(currentUserId).flatMapLatest { uids ->
            if (uids.isEmpty()) return@flatMapLatest flowOf(emptyList())
            
            val userFlows = uids.map { uid ->
                observeUser(uid).map { user ->
                    user?.let {
                        FriendUser(
                            uid = it.id,
                            displayName = it.displayName,
                            avatarId = it.avatarId,
                            major = it.major,
                            showOnlineStatus = it.showOnlineStatus
                        )
                    }
                }
            }
            
            combine(userFlows) { users ->
                users.filterNotNull()
            }
        }
    }

    private fun observeFriendUids(currentUserId: String): Flow<List<String>> = callbackFlow {
        val registration = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val uids = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("uid")
                } ?: emptyList()
                trySend(uids)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getFriends(currentUserId: String): List<FriendUser> {
        val snapshot = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val uid = doc.getString("uid") ?: return@mapNotNull null
            val user = getUserById(uid) ?: return@mapNotNull null
            FriendUser(
                uid = user.id,
                displayName = user.displayName,
                avatarId = user.avatarId,
                major = user.major,
                showOnlineStatus = user.showOnlineStatus
            )
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, fromUserId: String) {
        val batch = firestore.batch()

        val incomingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("incomingRequests")
            .document(fromUserId)

        val outgoingRef = firestore.collection("users")
            .document(fromUserId)
            .collection("outgoingRequests")
            .document(currentUserId)

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
        batch.set(currentFriendRef, mapOf("uid" to fromUserId))
        batch.set(fromFriendRef, mapOf("uid" to currentUserId))

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
