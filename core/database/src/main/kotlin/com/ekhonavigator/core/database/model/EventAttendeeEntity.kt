package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.ForeignKey
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus

@Entity(
    tableName = "event_attendees",
    primaryKeys = ["eventId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = CalendarEventEntity::class,
            parentColumns = ["uid"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EventAttendeeEntity(
    val eventId: String,
    val userId: String,
    val displayName: String,
    val rsvpStatus: RsvpStatus,
)

fun EventAttendeeEntity.toDomainModel(): EventAttendee = EventAttendee(
    userId = userId,
    displayName = displayName,
    rsvpStatus = rsvpStatus,
)
