package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.model.toEntity
import com.ekhonavigator.core.data.util.SyncResult
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.model.toDomainModel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.network.ICalFeedDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCalendarRepository @Inject constructor(
    private val calendarEventDao: CalendarEventDao,
    private val iCalFeedDataSource: ICalFeedDataSource,
) : CalendarRepository {

    override fun observeEvents(): Flow<List<CalendarEvent>> =
        calendarEventDao.observeAllEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeBookmarkedEvents(): Flow<List<CalendarEvent>> =
        calendarEventDao.observeBookmarkedEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeEventsByDateRange(
        start: Instant,
        end: Instant,
    ): Flow<List<CalendarEvent>> =
        calendarEventDao.observeEventsByDateRange(start, end).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeEventById(id: String): Flow<CalendarEvent?> =
        calendarEventDao.observeEventById(id).map { it?.toDomainModel() }

    override suspend fun toggleBookmark(eventId: String) {
        val event = calendarEventDao.getEventById(eventId) ?: return
        calendarEventDao.updateBookmark(eventId, !event.isBookmarked)
    }

    override suspend fun sync(feedUrl: String): SyncResult {
        return try {
            val networkEvents = iCalFeedDataSource.fetchEvents(feedUrl)
            val syncTime = Instant.now()

            // Preserve bookmark status for events that already exist locally
            val entities = networkEvents.map { networkEvent ->
                val existing = calendarEventDao.getEventById(networkEvent.uid)
                networkEvent.toEntity(
                    existingBookmark = existing?.isBookmarked ?: false,
                    syncedAt = syncTime,
                )
            }

            calendarEventDao.upsertEvents(entities)

            // Remove events no longer in the feed (but keep bookmarked ones)
            val activeUids = networkEvents.map { it.uid }
            calendarEventDao.deleteEventsNotIn(activeUids)

            SyncResult.Success(eventsUpdated = entities.size)
        } catch (e: Exception) {
            SyncResult.Error(
                message = e.message ?: "Unknown sync error",
                cause = e,
            )
        }
    }
}
