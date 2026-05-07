package com.ekhonavigator.core.canvas.network

interface CanvasApiProvider {
    /** Returns the API client for the currently connected Canvas account, or null when none. */
    fun current(): CanvasApi?
}
