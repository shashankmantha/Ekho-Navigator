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

    fun observeMyEvents(ownerUid: String): Flow<List<CalendarEvent>>

    fun observeSharedEvents(): Flow<List<CalendarEvent>>

    fun observeAttendees(eventId: String): Flow<List<EventAttendee>>

    /** Create a new custom event. Writes to Room immediately, then best-effort to Firestore.
     *  [sharedWith] maps friend UIDs to display names for participants with PENDING RSVP status. */
    suspend fun createEvent(
        event: CalendarEvent,
        sharedWith: Map<String, String> = emptyMap()
    ): String

    suspend fun updateEvent(event: CalendarEvent)

    suspend fun addAttendees(eventId: String, sharedWith: Map<String, String>)

    suspend fun deleteEvent(eventId: String)

    suspend fun rsvp(eventId: String, userId: String, displayName: String, status: RsvpStatus)

    suspend fun syncAttendees(eventId: String)

    /** Push all pendingSync events to Firestore. Called by a sync worker or on-demand. */
    suspend fun pushPendingEvents()

    /** Start listening for shared events from Firestore. Call on sign-in. */
    fun startSync(scope: CoroutineScope)

    /** Stop listening. Call on sign-out or app teardown. */
    fun stopSync()

    /** Stop listening and clear all user-specific events from Room. Call on sign-out. */
    suspend fun onSignOut()
}
