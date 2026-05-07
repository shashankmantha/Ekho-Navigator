package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.auth.CanvasIdentitySource
import com.ekhonavigator.core.data.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FirebaseCanvasIdentitySource @Inject constructor(
    private val authRepository: AuthRepository,
) : CanvasIdentitySource {
    override fun currentUid(): String? = authRepository.getCurrentUserUid()
}
