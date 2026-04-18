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
    /** Declined events (RSVP = NOT_GOING) are filtered out; use [observeDeclinedInvites] for undo access. */
    fun observeEvents(): Flow<List<CalendarEvent>>
    fun observeBookmarkedEvents(): Flow<List<CalendarEvent>>
    fun observeEventsByDateRange(start: Instant, end: Instant): Flow<List<CalendarEvent>>
    /** Does NOT filter declined events — the detail screen must still open them so the user can undo. */
    fun observeEventById(id: String): Flow<CalendarEvent?>
    fun observePendingInvites(): Flow<List<CalendarEvent>>
    fun observeDeclinedInvites(): Flow<List<CalendarEvent>>
    suspend fun toggleBookmark(eventId: String)
    suspend fun restoreBookmarks()
    suspend fun onSignOut()
    suspend fun sync(feedUrl: String): SyncResult
}
