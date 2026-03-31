package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake [CustomEventRepository] for unit tests.
 *
 * Tracks calls to [createEvent] and [deleteEvent] so tests can verify
 * the ViewModel delegates correctly.
 */
class TestCustomEventRepository : CustomEventRepository {

    val createdEvents = mutableListOf<Pair<CalendarEvent, Map<String, String>>>()
    val deletedEventIds = mutableListOf<String>()

    override fun observeMyEvents(ownerUid: String): Flow<List<CalendarEvent>> = flowOf(emptyList())

    override fun observeSharedEvents(): Flow<List<CalendarEvent>> = flowOf(emptyList())

    override fun observeAttendees(eventId: String): Flow<List<EventAttendee>> = flowOf(emptyList())

    override suspend fun createEvent(event: CalendarEvent, sharedWith: Map<String, String>): String {
        createdEvents += event to sharedWith
        return "test-event-id"
    }

    override suspend fun updateEvent(event: CalendarEvent) {}

    override suspend fun deleteEvent(eventId: String) {
        deletedEventIds += eventId
    }

    override suspend fun rsvp(eventId: String, userId: String, displayName: String, status: RsvpStatus) {}

    override suspend fun syncAttendees(eventId: String) {}

    override suspend fun pushPendingEvents() {}

    override fun startSync(scope: CoroutineScope) {}

    override fun stopSync() {}

    override suspend fun onSignOut() {}
}
