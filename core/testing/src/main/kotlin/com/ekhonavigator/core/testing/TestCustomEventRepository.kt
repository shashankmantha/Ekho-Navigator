package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Fake [CustomEventRepository] for unit tests.
 *
 * Tracks calls to [createEvent] and [deleteEvent] so tests can verify
 * the ViewModel delegates correctly.
 */
class TestCustomEventRepository : CustomEventRepository {

    val createdEvents = mutableListOf<Pair<CalendarEvent, Map<String, String>>>()
    val deletedEventIds = mutableListOf<String>()

    data class RsvpCall(
        val eventId: String,
        val userId: String,
        val displayName: String,
        val status: RsvpStatus,
    )

    val rsvpCalls = mutableListOf<RsvpCall>()

    override fun observeMyEvents(ownerUid: String): Flow<List<CalendarEvent>> = flowOf(emptyList())

    override fun observeSharedEvents(): Flow<List<CalendarEvent>> = flowOf(emptyList())

    /** Per-event attendee snapshots. Tests seed via [setAttendees] so the ViewModel's
     *  edit-mode load can pick up a non-empty initial set. */
    private val attendeesByEvent = MutableStateFlow<Map<String, List<EventAttendee>>>(emptyMap())

    fun setAttendees(eventId: String, attendees: List<EventAttendee>) {
        attendeesByEvent.value = attendeesByEvent.value + (eventId to attendees)
    }

    override fun observeAttendees(eventId: String): Flow<List<EventAttendee>> =
        attendeesByEvent.map { it[eventId].orEmpty() }

    override suspend fun createEvent(
        event: CalendarEvent,
        sharedWith: Map<String, String>
    ): String {
        createdEvents += event to sharedWith
        return "test-event-id"
    }

    val updatedEvents = mutableListOf<CalendarEvent>()

    override suspend fun updateEvent(event: CalendarEvent) {
        updatedEvents += event
    }

    val addedAttendees = mutableListOf<Pair<String, Map<String, String>>>()

    override suspend fun addAttendees(eventId: String, sharedWith: Map<String, String>) {
        addedAttendees += eventId to sharedWith
    }

    val removedAttendees = mutableListOf<Pair<String, String>>()

    override suspend fun removeAttendee(eventId: String, userId: String) {
        removedAttendees += eventId to userId
    }

    override suspend fun deleteEvent(eventId: String) {
        deletedEventIds += eventId
    }

    override suspend fun rsvp(
        eventId: String,
        userId: String,
        displayName: String,
        status: RsvpStatus
    ) {
        rsvpCalls += RsvpCall(eventId, userId, displayName, status)
    }

    override suspend fun syncAttendees(eventId: String) {}

    override suspend fun pushPendingEvents() {}

    override fun startSync(scope: CoroutineScope) {}

    override fun stopSync() {}

    override suspend fun onSignOut() {}
}
