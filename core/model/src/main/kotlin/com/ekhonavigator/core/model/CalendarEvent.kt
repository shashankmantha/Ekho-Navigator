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
    val ownerDisplayName: String = "",
    val pendingSync: Boolean = false,
    val myRsvpStatus: RsvpStatus? = null,
    val eventName: String = "",
    val organization: String = "",
    val eventType: String = "",
    val placeId: String? = null,
    val externalSourceId: String? = null,
    val externalSourceType: String? = null,
    val dueAt: Instant? = null,
    /** Denormalized snapshot of a user-marker's coordinates and label, populated when
     *  the event is pinned to a custom marker. Lets recipients of a shared event resolve
     *  the location without owning the source marker — they can save it as their own. */
    val customLocation: SharedLocation? = null,
) {
    val primaryCategory: EventCategory
        get() = categories.firstOrNull() ?: EventCategory.GENERAL
}
