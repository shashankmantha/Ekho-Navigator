package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.model.toCustomEventEntity
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import com.ekhonavigator.core.database.model.EventAttendeeEntity
import com.ekhonavigator.core.database.model.toDomainModel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCustomEventRepository @Inject constructor(
    private val calendarEventDao: CalendarEventDao,
    private val eventAttendeeDao: EventAttendeeDao,
) : CustomEventRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun observeMyEvents(ownerUid: String): Flow<List<CalendarEvent>> =
        calendarEventDao.observeMyEvents(ownerUid).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeSharedEvents(): Flow<List<CalendarEvent>> =
        calendarEventDao.observeSharedEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeAttendees(eventId: String): Flow<List<EventAttendee>> =
        eventAttendeeDao.observeAttendees(eventId).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override suspend fun createEvent(event: CalendarEvent, sharedWithUids: Set<String>): String {
        val eventId = "custom_${UUID.randomUUID()}"
        val entity = event.toCustomEventEntity(eventId)
        calendarEventDao.upsertEvent(entity)

        // Write attendee entities to Room for each shared friend
        for (uid in sharedWithUids) {
            eventAttendeeDao.upsertAttendee(
                EventAttendeeEntity(
                    eventId = eventId,
                    userId = uid,
                    displayName = "",
                    rsvpStatus = RsvpStatus.PENDING,
                ),
            )
        }

        try {
            val allParticipants = listOfNotNull(event.ownerUid) + sharedWithUids
            pushEventToFirestore(eventId, event, allParticipants)

            // Create attendee docs in Firestore for each shared friend
            for (uid in sharedWithUids) {
                firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(uid)
                    .set(mapOf("rsvpStatus" to RsvpStatus.PENDING.name))
                    .await()
            }

            calendarEventDao.updatePendingSync(eventId, false)
        } catch (_: Exception) {
            // Stays pendingSync = true, will be retried by pushPendingEvents()
        }
        return eventId
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        val entity = event.toCustomEventEntity(event.id).copy(pendingSync = true)
        calendarEventDao.upsertEvent(entity)

        try {
            pushEventToFirestore(event.id, event)
            calendarEventDao.updatePendingSync(event.id, false)
        } catch (_: Exception) {
            // Retry later
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        calendarEventDao.deleteEvent(eventId)
        try {
            firestore.collection("events").document(eventId).delete().await()
        } catch (_: Exception) {
            // Event is already gone from Room
        }
    }

    override suspend fun rsvp(
        eventId: String,
        userId: String,
        displayName: String,
        status: RsvpStatus,
    ) {
        eventAttendeeDao.upsertAttendee(
            EventAttendeeEntity(
                eventId = eventId,
                userId = userId,
                displayName = displayName,
                rsvpStatus = status,
            ),
        )
        try {
            firestore.collection("events")
                .document(eventId)
                .collection("attendees")
                .document(userId)
                .set(
                    mapOf(
                        "displayName" to displayName,
                        "rsvpStatus" to status.name,
                    ),
                )
                .await()
        } catch (_: Exception) {
            // Offline-first: Room has the data
        }
    }

    override suspend fun pushPendingEvents() {
        val pending = calendarEventDao.getPendingSyncEvents()
        for (entity in pending) {
            try {
                pushEventToFirestore(entity.uid, entity.toDomainModel())
                calendarEventDao.updatePendingSync(entity.uid, false)
            } catch (_: Exception) {
                // Skip, retry next time
            }
        }
    }

    private suspend fun pushEventToFirestore(
        eventId: String,
        event: CalendarEvent,
        participants: List<String> = listOfNotNull(event.ownerUid),
    ) {
        val data = mapOf(
            "ownerUid" to event.ownerUid,
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "startTime" to event.startTime.toEpochMilli(),
            "endTime" to event.endTime.toEpochMilli(),
            "categories" to event.categories.map { it.name },
            "participants" to participants,
            "source" to event.source.name,
            "createdAt" to com.google.firebase.Timestamp.now(),
        )
        firestore.collection("events").document(eventId).set(data).await()
    }
}
