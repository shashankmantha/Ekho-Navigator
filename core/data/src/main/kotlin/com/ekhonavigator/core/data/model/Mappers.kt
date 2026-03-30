package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.network.model.NetworkCalendarEvent
import java.time.Instant

/**
 * Maps a network event to a database entity.
 * [existingBookmark] preserves the user's bookmark if the event already exists locally.
 */
fun NetworkCalendarEvent.toEntity(
    existingBookmark: Boolean = false,
    syncedAt: Instant = Instant.now(),
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
)
