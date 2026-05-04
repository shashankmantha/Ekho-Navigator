package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
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
    type = type,
    courseLabel = courseLabel,
)

/** [source] defaults to SHARED but is overridden when an owner's second device receives their own event back through the listener. */
fun firestoreDocToEntity(
    doc: DocumentSnapshot,
    source: EventSource = EventSource.SHARED,
): CalendarEventEntity? = firestoreDataToEntity(doc.id, doc.data, source)

/**
 * Map-based variant of [firestoreDocToEntity] — exists so JVM unit tests can cover the
 * field parsing without constructing a real [DocumentSnapshot] (the SDK doesn't expose
 * a public builder, and mocking Firestore types is heavier than the round-trip is worth).
 */
internal fun firestoreDataToEntity(
    id: String,
    data: Map<String, Any?>?,
    source: EventSource = EventSource.SHARED,
): CalendarEventEntity? {
    if (data == null) return null
    val title = data["title"] as? String ?: return null
    val startMillis = (data["startTime"] as? Number)?.toLong() ?: return null
    val endMillis = (data["endTime"] as? Number)?.toLong() ?: return null

    val categoryNames = data["categories"] as? List<*> ?: emptyList<String>()
    val categories = categoryNames.mapNotNull { name ->
        try {
            EventCategory.valueOf(name as String)
        } catch (_: Exception) {
            null
        }
    }.ifEmpty { listOf(EventCategory.GENERAL) }

    val customLocation = data["customLocation"] as? Map<*, *>
    val customLocationTitle = customLocation?.get("title") as? String
    val customLocationLat = (customLocation?.get("latitude") as? Number)?.toDouble()
    val customLocationLng = (customLocation?.get("longitude") as? Number)?.toDouble()

    return CalendarEventEntity(
        uid = id,
        title = title,
        description = data["description"] as? String ?: "",
        location = data["location"] as? String ?: "",
        startTime = Instant.ofEpochMilli(startMillis),
        endTime = Instant.ofEpochMilli(endMillis),
        categories = categories,
        url = "",
        status = "CONFIRMED",
        isBookmarked = true,
        lastSyncedAt = Instant.now(),
        source = source,
        ownerUid = data["ownerUid"] as? String,
        ownerDisplayName = data["ownerDisplayName"] as? String ?: "",
        pendingSync = false,
        eventName = data["eventName"] as? String ?: "",
        organization = data["organization"] as? String ?: "",
        eventType = data["eventType"] as? String ?: "",
        placeId = data["placeId"] as? String,
        customLocationTitle = customLocationTitle,
        customLocationLatitude = customLocationLat,
        customLocationLongitude = customLocationLng,
        type = (data["type"] as? String)?.let { name ->
            try {
                EventType.valueOf(name)
            } catch (_: IllegalArgumentException) {
                EventType.EVENT
            }
        } ?: EventType.EVENT,
        courseLabel = data["courseLabel"] as? String,
    )
}
