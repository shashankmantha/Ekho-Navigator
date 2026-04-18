package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant

/**
 * Maps a network event to a database entity.
 * [existingBookmark] preserves the user's bookmark if the event already exists locally.
 */
fun NetworkCalendarEvent.toEntity(
    existingBookmark: Boolean = false,
    syncedAt: Instant = Instant.now(),
    placeId: String? = null,
): CalendarEventEntity = CalendarEventEntity(
    uid = uid,
    title = summary,
    description = description,
    location = location,
    startTime = dtStart,
    endTime = dtEnd,
    categories = categories,
    url = url,
    status = status,
    isBookmarked = existingBookmark,
    lastSyncedAt = syncedAt,
    eventName = eventName,
    organization = organization,
    eventType = eventType,
    placeId = placeId,
)

/**
 * Maps a domain [CalendarEvent] to a database entity for user-created events.
 * Sets [EventSource.USER_CREATED] and marks as pending sync by default.
 * Always bookmarked — the user owns this event, so it's inherently "saved".
 */
fun CalendarEvent.toCustomEventEntity(
    eventId: String = id,
): CalendarEventEntity = CalendarEventEntity(
    uid = eventId,
    title = title,
    description = description,
    location = location,
    startTime = startTime,
    endTime = endTime,
    categories = categories,
    url = url,
    status = status,
    isBookmarked = true,
    lastSyncedAt = Instant.now(),
    source = EventSource.USER_CREATED,
    ownerUid = ownerUid,
    pendingSync = true,
    eventName = eventName,
    organization = organization,
    eventType = eventType,
    placeId = placeId,
)

/**
 * Maps a Firestore event document to a [CalendarEventEntity].
 * [source] defaults to [EventSource.SHARED] but can be overridden for owned events
 * syncing to a second device.
 * Returns null if required fields are missing.
 */
fun firestoreDocToEntity(
    doc: DocumentSnapshot,
    source: EventSource = EventSource.SHARED,
): CalendarEventEntity? {
    val title = doc.getString("title") ?: return null
    val startMillis = doc.getLong("startTime") ?: return null
    val endMillis = doc.getLong("endTime") ?: return null

    val categoryNames = doc.get("categories") as? List<*> ?: emptyList<String>()
    val categories = categoryNames.mapNotNull { name ->
        try {
            EventCategory.valueOf(name as String)
        } catch (_: Exception) {
            null
        }
    }.ifEmpty { listOf(EventCategory.GENERAL) }

    return CalendarEventEntity(
        uid = doc.id,
        title = title,
        description = doc.getString("description") ?: "",
        location = doc.getString("location") ?: "",
        startTime = Instant.ofEpochMilli(startMillis),
        endTime = Instant.ofEpochMilli(endMillis),
        categories = categories,
        url = "",
        status = "CONFIRMED",
        isBookmarked = true,
        lastSyncedAt = Instant.now(),
        source = source,
        ownerUid = doc.getString("ownerUid"),
        pendingSync = false,
        eventName = doc.getString("eventName") ?: "",
        organization = doc.getString("organization") ?: "",
        eventType = doc.getString("eventType") ?: "",
    )
}
