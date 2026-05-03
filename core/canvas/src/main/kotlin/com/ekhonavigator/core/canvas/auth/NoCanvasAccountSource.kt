package com.ekhonavigator.core.canvas.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder until the Connect Canvas screen wires Firebase auth to a saved
 * institution domain. Replace the binding in CanvasModule when that lands.
 */
@Singleton
internal class NoCanvasAccountSource @Inject constructor() : CanvasAccountSource {
    override fun currentOrNull(): CanvasAccount? = null
}
