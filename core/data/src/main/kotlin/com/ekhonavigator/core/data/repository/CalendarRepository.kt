package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.util.SyncResult
import com.ekhonavigator.core.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Single source of truth for calendar event data.
 * UI layers depend on this interface — never on database or network directly.
 */
interface CalendarRepository {
    fun observeEvents(): Flow<List<CalendarEvent>>
    fun observeBookmarkedEvents(): Flow<List<CalendarEvent>>
    fun observeEventsByDateRange(start: Instant, end: Instant): Flow<List<CalendarEvent>>
    fun observeEventById(id: String): Flow<CalendarEvent?>
    suspend fun toggleBookmark(eventId: String)
    suspend fun sync(feedUrl: String): SyncResult
}
