package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user-created custom events stored in Firestore.
 * Room is the single source of truth; Firestore is the remote backing store.
 */
interface CustomEventRepository {

    /** Observe events owned by the given user. */
    fun observeMyEvents(ownerUid: String): Flow<List<CalendarEvent>>

    /** Observe events shared with the current user. */
    fun observeSharedEvents(): Flow<List<CalendarEvent>>

    /** Observe attendees for a specific event. */
    fun observeAttendees(eventId: String): Flow<List<EventAttendee>>

    /** Create a new custom event. Writes to Room immediately, then best-effort to Firestore.
     *  [sharedWithUids] are friend UIDs to add as participants with PENDING RSVP status. */
    suspend fun createEvent(event: CalendarEvent, sharedWithUids: Set<String> = emptySet()): String

    /** Update an existing custom event. */
    suspend fun updateEvent(event: CalendarEvent)

    /** Delete a custom event from Room and Firestore. */
    suspend fun deleteEvent(eventId: String)

    /** RSVP to an event. */
    suspend fun rsvp(eventId: String, userId: String, displayName: String, status: RsvpStatus)

    /** Pull latest attendees from Firestore into Room for a specific event. */
    suspend fun syncAttendees(eventId: String)

    /** Push all pendingSync events to Firestore. Called by a sync worker or on-demand. */
    suspend fun pushPendingEvents()

    /** Start listening for shared events from Firestore. Call on sign-in. */
    fun startSync(scope: CoroutineScope)

    /** Stop listening. Call on sign-out or app teardown. */
    fun stopSync()
}
