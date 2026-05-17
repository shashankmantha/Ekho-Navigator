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
    val type: EventType = EventType.EVENT,
    /** Course code tag (e.g. "COMP-262"). Personal events created by the user
     *  can be tied to a course; family-key extraction (`CourseColorAssigner.familyKey()`)
     *  drives the visual color, so a personal event tagged "COMP-262" matches
     *  the Canvas COMP-262 course's palette slot — no Canvas link required.
     *  Null = no course tag. */
    val courseLabel: String? = null,
    /** User-toggled completion state for personal ASSIGNMENT events. Meaningful
     *  only for USER_CREATED + type=ASSIGNMENT — Canvas assignments use
     *  submitted/graded/excused on the planner item table for the same purpose. */
    val isCompleted: Boolean = false,
    /** Set when this event repeats weekly until [RecurrenceRule.endDate]. Null
     *  for one-off events. Expansion happens at observation time so we keep one
     *  row per series in storage rather than thousands of instances. */
    val recurrence: RecurrenceRule? = null,
) {
    val primaryCategory: EventCategory
        get() = categories.firstOrNull() ?: EventCategory.GENERAL
}
