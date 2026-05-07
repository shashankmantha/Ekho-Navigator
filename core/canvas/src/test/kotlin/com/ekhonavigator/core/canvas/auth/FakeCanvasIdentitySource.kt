package com.ekhonavigator.core.canvas.auth

class FakeCanvasIdentitySource(var uid: String? = null) : CanvasIdentitySource {
    override fun currentUid(): String? = uid
}
