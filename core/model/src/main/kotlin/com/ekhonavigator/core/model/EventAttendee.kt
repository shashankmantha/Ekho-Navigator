package com.ekhonavigator.core.model

data class EventAttendee(
    val userId: String,
    val displayName: String,
    val rsvpStatus: RsvpStatus,
)
