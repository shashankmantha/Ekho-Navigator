package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.SharedLocation
import java.time.Instant

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey
    val uid: String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: Instant,
    val endTime: Instant,
    val categories: List<EventCategory>,
    val url: String,
    val status: String,
    val isBookmarked: Boolean = false,
    val lastSyncedAt: Instant,
    val source: EventSource = EventSource.ICAL_FEED,
    val ownerUid: String? = null,
    val ownerDisplayName: String = "",
    val pendingSync: Boolean = false,
    val eventName: String = "",
    val organization: String = "",
    val eventType: String = "",
    val placeId: String? = null,
    val externalSourceId: String? = null,
    val externalSourceType: String? = null,
    val dueAt: Instant? = null,
    // Flat columns rather than a TypeConverter — Room rejects nested @Embedded on
    // optional values, and three nullable doubles round-trip to a SharedLocation cleanly.
    val customLocationTitle: String? = null,
    val customLocationLatitude: Double? = null,
    val customLocationLongitude: Double? = null,
    val type: EventType = EventType.EVENT,
)

fun CalendarEventEntity.isPast(now: Instant = Instant.now()): Boolean = endTime <= now

fun CalendarEventEntity.toDomainModel(
    myRsvpStatus: RsvpStatus? = null,
): CalendarEvent = CalendarEvent(
    id = uid,
    title = title,
    description = description,
    location = location,
    startTime = startTime,
    endTime = endTime,
    categories = categories,
    url = url,
    status = status,
    isBookmarked = isBookmarked,
    lastSyncedAt = lastSyncedAt,
    source = source,
    ownerUid = ownerUid,
    ownerDisplayName = ownerDisplayName,
    pendingSync = pendingSync,
    myRsvpStatus = myRsvpStatus,
    eventName = eventName,
    organization = organization,
    eventType = eventType,
    placeId = placeId,
    externalSourceId = externalSourceId,
    externalSourceType = externalSourceType,
    dueAt = dueAt,
    customLocation = customLocationTitle?.let { title ->
        val lat = customLocationLatitude
        val lng = customLocationLongitude
        if (lat != null && lng != null) SharedLocation(title, lat, lng) else null
    },
    type = type,
)
