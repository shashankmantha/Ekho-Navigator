package com.ekhonavigator.core.model

data class PresenceStatus(
    val state: String = "offline",
    val lastChanged: Long = 0L,
)
