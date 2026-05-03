package com.ekhonavigator.core.canvas.auth

interface CanvasTokenStore {
    fun get(account: CanvasAccount): String?
    fun put(account: CanvasAccount, token: String)
    fun delete(account: CanvasAccount)
    fun deleteAll()
}
