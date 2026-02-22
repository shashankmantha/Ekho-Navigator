package com.ekhonavigator.core.data.model

import com.ekhonavigator.core.database.model.CalendarEventEntity
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
