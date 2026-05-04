package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.auth.CanvasTokenStore
import com.ekhonavigator.core.data.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "is the current Firebase user actively connected to
 * Canvas?" — i.e. signed in AND has a stored institution AND has a stored PAT
 * for that account. Exposed as a Flow so any UI surface can react.
 *
 * Drives the [com.ekhonavigator.core.designsystem.theme.LocalCanvasConnected]
 * CompositionLocal at the app root, which gates Canvas-only chrome (course
 * filter chips, Canvas source toggle, future per-class detail entry points).
 */
@Singleton
class CanvasConnectionState @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountSource: CanvasAccountSource,
    private val tokenStore: CanvasTokenStore,
) {
    val isConnected: Flow<Boolean> = combine(
        authRepository.userFlow(),
        tokenStore.changes(),
    ) { uid, _ ->
        if (uid == null) return@combine false
        val account = accountSource.currentOrNull() ?: return@combine false
        !tokenStore.get(account).isNullOrBlank()
    }
        .map { it }
        .distinctUntilChanged()
}
