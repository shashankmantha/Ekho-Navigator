package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.EventAttendeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventAttendeeDao {

    @Query("SELECT * FROM event_attendees WHERE eventId = :eventId")
    fun observeAttendees(eventId: String): Flow<List<EventAttendeeEntity>>

    @Query("SELECT * FROM event_attendees WHERE userId = :userId")
    fun observeAllForUser(userId: String): Flow<List<EventAttendeeEntity>>

    @Query("SELECT * FROM event_attendees WHERE eventId = :eventId AND userId = :userId LIMIT 1")
    suspend fun getAttendee(eventId: String, userId: String): EventAttendeeEntity?

    @Upsert
    suspend fun upsertAttendee(attendee: EventAttendeeEntity)

    @Query("DELETE FROM event_attendees WHERE eventId = :eventId AND userId = :userId")
    suspend fun removeAttendee(eventId: String, userId: String)

    @Query("DELETE FROM event_attendees WHERE eventId = :eventId")
    suspend fun deleteAllForEvent(eventId: String)
}
