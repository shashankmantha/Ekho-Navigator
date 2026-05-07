package com.ekhonavigator.core.canvas.auth

class FakeCanvasAccountSource(var account: CanvasAccount? = null) : CanvasAccountSource {
    override fun currentOrNull(): CanvasAccount? = account
}
