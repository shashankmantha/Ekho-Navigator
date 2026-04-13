package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.model.PresenceStatus
import kotlinx.coroutines.flow.Flow

interface PresenceRepository {
    /**
     * Sets up the onDisconnect handlers and marks the user as online.
     * Should be called when the user signs in or the app starts while signed in.
     */
    fun startPresence(uid: String)

    /**
     * Stops tracking presence for the current user.
     */
    fun stopPresence()

    /**
     * Manually marks the user as offline (e.g. during sign out).
     */
    suspend fun setOfflineNow(uid: String)

    /**
     * Observes the presence of another user.
     */
    fun observePresence(uid: String): Flow<PresenceStatus>
}
