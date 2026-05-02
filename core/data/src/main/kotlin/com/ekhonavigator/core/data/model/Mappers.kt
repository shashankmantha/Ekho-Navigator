package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant

/** [existingBookmark] preserves the user's bookmark across re-syncs — the iCal feed has no awareness of it. */
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

/** [isBookmarked] is forced true: the user owns this event, so "saved" is implicit — they can't un-save their own creation. */
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
    ownerDisplayName = ownerDisplayName,
    pendingSync = true,
    eventName = eventName,
    organization = organization,
    eventType = eventType,
    placeId = placeId,
    externalSourceId = externalSourceId,
    externalSourceType = externalSourceType,
    dueAt = dueAt,
    customLocationTitle = customLocation?.title,
    customLocationLatitude = customLocation?.latitude,
    customLocationLongitude = customLocation?.longitude,
)

/** [source] defaults to SHARED but is overridden when an owner's second device receives their own event back through the listener. */
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

    val customLocation = doc.get("customLocation") as? Map<*, *>
    val customLocationTitle = customLocation?.get("title") as? String
    val customLocationLat = (customLocation?.get("latitude") as? Number)?.toDouble()
    val customLocationLng = (customLocation?.get("longitude") as? Number)?.toDouble()

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
        ownerDisplayName = doc.getString("ownerDisplayName") ?: "",
        pendingSync = false,
        eventName = doc.getString("eventName") ?: "",
        organization = doc.getString("organization") ?: "",
        eventType = doc.getString("eventType") ?: "",
        placeId = doc.getString("placeId"),
        customLocationTitle = customLocationTitle,
        customLocationLatitude = customLocationLat,
        customLocationLongitude = customLocationLng,
    )
}
