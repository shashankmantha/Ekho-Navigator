package com.ekhonavigator.core.model

import java.time.Instant

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: Instant,
    val endTime: Instant,
    val categories: List<EventCategory>,
    val url: String,
    val status: String,
    val isBookmarked: Boolean,
    val lastSyncedAt: Instant,
    val source: EventSource = EventSource.ICAL_FEED,
    val ownerUid: String? = null,
    val pendingSync: Boolean = false,
    val myRsvpStatus: RsvpStatus? = null,
) {
    val primaryCategory: EventCategory
        get() = categories.firstOrNull() ?: EventCategory.GENERAL
}
