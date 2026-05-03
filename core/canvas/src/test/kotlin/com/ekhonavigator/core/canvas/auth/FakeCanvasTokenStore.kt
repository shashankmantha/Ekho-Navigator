package com.ekhonavigator.core.canvas.auth

class FakeCanvasTokenStore : CanvasTokenStore {
    private val tokens = mutableMapOf<CanvasAccount, String>()

    override fun get(account: CanvasAccount): String? = tokens[account]

    override fun put(account: CanvasAccount, token: String) {
        tokens[account] = token
    }

    override fun delete(account: CanvasAccount) {
        tokens.remove(account)
    }

    override fun deleteAll() {
        tokens.clear()
    }
}
