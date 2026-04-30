package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.FriendUser
import com.ekhonavigator.core.data.social.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake [SocialRepository] for unit tests.
 *
 * Extends the real repository so it can be passed anywhere a `SocialRepository`
 * is expected. Firestore-backed methods on the parent are never reached because
 * the test overrides shadow them and the parent's `firestore` field is lazy
 * — constructing this fake does not touch Firebase.
 */
class TestSocialRepository : SocialRepository() {
    private val incomingRequestsFlow = MutableStateFlow<List<FriendRequest>>(emptyList())
    var friends: List<FriendUser> = emptyList()

    fun emitIncomingRequests(requests: List<FriendRequest>) {
        incomingRequestsFlow.value = requests
    }

    override fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> =
        incomingRequestsFlow

    override suspend fun getFriends(currentUserId: String): List<FriendUser> = friends
}
