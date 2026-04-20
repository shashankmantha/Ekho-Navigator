package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.social.FriendRequest
import com.ekhonavigator.core.data.social.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake [SocialRepository] for unit tests.
 *
 * Extends the real repository so it can be passed anywhere a `SocialRepository`
 * is expected. Only [observeIncomingRequests] is overridden — the parent's
 * Firestore-backed methods stay intact but are never reached in tests. The
 * parent's `firestore` field is lazy, so constructing this fake does not
 * touch Firebase.
 *
 * Tests push friend-request lists through [emitIncomingRequests]; any
 * collector (a ViewModel using `stateIn` / `combine`) sees the emission
 * immediately via [MutableStateFlow].
 */
class TestSocialRepository : SocialRepository() {
    private val incomingRequestsFlow = MutableStateFlow<List<FriendRequest>>(emptyList())

    fun emitIncomingRequests(requests: List<FriendRequest>) {
        incomingRequestsFlow.value = requests
    }

    override fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> =
        incomingRequestsFlow
}
