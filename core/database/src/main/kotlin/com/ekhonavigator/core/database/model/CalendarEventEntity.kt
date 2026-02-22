package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
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
)

fun CalendarEventEntity.toDomainModel(): CalendarEvent = CalendarEvent(
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
)
