package com.ekhonavigator.core.network.model

import com.ekhonavigator.core.model.EventCategory
import java.time.Instant

/**
 * Network-layer representation of a calendar event parsed from an iCal feed.
 * Intentionally separate from the domain model — this is what comes off the wire,
 * before any local state (like bookmarks) is applied.
 */
data class NetworkCalendarEvent(
    val uid: String,
    val summary: String,
    val eventName: String,
    val description: String,
    val location: String,
    val organization: String,
    val eventType: String,
    val dtStart: Instant,
    val dtEnd: Instant,
    val categories: List<EventCategory>,
    val url: String,
    val status: String,
)
