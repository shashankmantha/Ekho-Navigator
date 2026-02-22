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
) {
    val primaryCategory: EventCategory
        get() = categories.firstOrNull() ?: EventCategory.GENERAL
}
