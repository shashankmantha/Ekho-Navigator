package com.ekhonavigator.core.canvas.auth

import kotlinx.coroutines.flow.Flow

interface CanvasTokenStore {
    fun get(account: CanvasAccount): String?
    fun put(account: CanvasAccount, token: String)
    fun delete(account: CanvasAccount)
    fun deleteAll()

    /**
     * Hot stream that emits every time the store mutates (put / delete / deleteAll),
     * plus a replay-1 emission on subscribe so collectors don't miss the current state.
     * Drives auth-aware UI gating without requiring callers to remember to notify.
     */
    fun changes(): Flow<Unit>
}
