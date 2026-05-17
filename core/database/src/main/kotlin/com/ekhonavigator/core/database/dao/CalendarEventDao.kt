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

    // One seed row per class-meeting series; expansion happens in the repo.
    // Series is in-range when it started before the window ends AND its end
    // date hasn't already passed by the window start.
    @Query(
        """
        SELECT * FROM calendar_events
        WHERE recurrenceDaysOfWeek IS NOT NULL
        AND recurrenceEndDateEpochDay >= :rangeStartEpochDay
        AND startTime < :rangeEnd
        ORDER BY startTime ASC
        """
    )
    fun observeRecurringEventsInRange(
        rangeStartEpochDay: Long,
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

    /** Targeted description update used by the Canvas assignment-sync description
     *  backfill — writes assignment.description onto the bridged calendar_events
     *  row without disturbing other fields (including the user's bookmark state). */
    @Query("UPDATE calendar_events SET description = :description WHERE uid = :id")
    suspend fun updateDescription(id: String, description: String)

    @Upsert
    suspend fun upsertEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE source = 'ICAL_FEED' AND uid NOT IN (:activeUids)")
    suspend fun deleteICalEventsNotIn(activeUids: List<String>)

    @Query("DELETE FROM calendar_events WHERE endTime < :cutoff AND isBookmarked = 0")
    suspend fun deleteOldEvents(cutoff: Instant)

    @Query("SELECT * FROM calendar_events WHERE source = 'USER_CREATED' AND ownerUid = :ownerUid ORDER BY startTime ASC")
    fun observeMyEvents(ownerUid: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE source = 'SHARED' ORDER BY startTime ASC")
    fun observeSharedEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE pendingSync = 1")
    suspend fun getPendingSyncEvents(): List<CalendarEventEntity>

    @Query("UPDATE calendar_events SET pendingSync = :pending WHERE uid = :id")
    suspend fun updatePendingSync(id: String, pending: Boolean)

    @Query("DELETE FROM calendar_events WHERE uid = :id")
    suspend fun deleteEvent(id: String)

    @Query("DELETE FROM calendar_events WHERE source != 'ICAL_FEED'")
    suspend fun deleteAllUserEvents()

    @Query("UPDATE calendar_events SET isBookmarked = 0 WHERE isBookmarked = 1")
    suspend fun clearAllBookmarks()

    @Query(
        """
        DELETE FROM calendar_events
        WHERE externalSourceType = :sourceType
          AND startTime >= :rangeStart AND startTime < :rangeEnd
          AND uid NOT IN (:keepUids)
        """
    )
    suspend fun deleteByExternalSourceInRangeExcept(
        sourceType: String,
        rangeStart: Instant,
        rangeEnd: Instant,
        keepUids: List<String>,
    )

    @Query("DELETE FROM calendar_events WHERE externalSourceType = :sourceType")
    suspend fun deleteByExternalSource(sourceType: String)
}
