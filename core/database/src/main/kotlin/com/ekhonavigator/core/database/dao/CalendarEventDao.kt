package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.CalendarEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun observeAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE isBookmarked = 1 ORDER BY startTime ASC")
    fun observeBookmarkedEvents(): Flow<List<CalendarEventEntity>>

    @Query(
        """
        SELECT * FROM calendar_events
        WHERE startTime >= :rangeStart AND startTime < :rangeEnd
        ORDER BY startTime ASC
        """
    )
    fun observeEventsByDateRange(
        rangeStart: Instant,
        rangeEnd: Instant,
    ): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE uid = :id")
    fun observeEventById(id: String): Flow<CalendarEventEntity?>

    @Query("SELECT * FROM calendar_events WHERE uid = :id")
    suspend fun getEventById(id: String): CalendarEventEntity?

    @Upsert
    suspend fun upsertEvents(events: List<CalendarEventEntity>)

    @Query("UPDATE calendar_events SET isBookmarked = :bookmarked WHERE uid = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean)

    @Query("DELETE FROM calendar_events WHERE uid NOT IN (:activeUids)")
    suspend fun deleteEventsNotIn(activeUids: List<String>)

    @Query("DELETE FROM calendar_events WHERE endTime < :cutoff AND isBookmarked = 0")
    suspend fun deleteOldEvents(cutoff: Instant)
}
