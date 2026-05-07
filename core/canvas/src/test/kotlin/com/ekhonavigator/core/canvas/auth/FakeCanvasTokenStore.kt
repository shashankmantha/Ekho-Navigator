package com.ekhonavigator.core.canvas.auth

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeCanvasTokenStore : CanvasTokenStore {
    private val tokens = mutableMapOf<CanvasAccount, String>()
    private val mutations = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    override fun get(account: CanvasAccount): String? = tokens[account]

    override fun put(account: CanvasAccount, token: String) {
        tokens[account] = token
        mutations.tryEmit(Unit)
    }

    override fun delete(account: CanvasAccount) {
        tokens.remove(account)
        mutations.tryEmit(Unit)
    }

    override fun deleteAll() {
        tokens.clear()
        mutations.tryEmit(Unit)
    }

    override fun changes(): Flow<Unit> = mutations.asSharedFlow()
}
