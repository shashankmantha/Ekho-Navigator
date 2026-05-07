package com.ekhonavigator.core.canvas.auth

interface CanvasAccountSource {
    fun currentOrNull(): CanvasAccount?
}
