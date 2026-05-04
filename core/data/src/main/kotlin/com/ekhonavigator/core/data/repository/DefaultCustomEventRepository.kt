package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.auth.NotSignedInException
import com.ekhonavigator.core.data.model.toCustomEventEntity
import com.ekhonavigator.core.data.sync.SharedEventSyncService
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import com.ekhonavigator.core.database.model.EventAttendeeEntity
import com.ekhonavigator.core.database.model.toDomainModel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
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
    private val sharedEventSyncService: SharedEventSyncService,
    private val authRepository: AuthRepository,
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

    override suspend fun createEvent(
        event: CalendarEvent,
        sharedWith: Map<String, String>
    ): String {
        // Refuse early when signed out: events are inherently user-owned, and a
        // Room-only write here would create a zombie local event with no ownerUid
        // that could never sync. UI gating (FAB grey when signed out) keeps this
        // from firing in practice — this is the race-window safety net.
        authRepository.getCurrentUserUid() ?: throw NotSignedInException()
        val eventId = "custom_${UUID.randomUUID()}"
        val entity = event.toCustomEventEntity(eventId)
        calendarEventDao.upsertEvent(entity)

        for ((uid, displayName) in sharedWith) {
            eventAttendeeDao.upsertAttendee(
                EventAttendeeEntity(
                    eventId = eventId,
                    userId = uid,
                    displayName = displayName,
                    rsvpStatus = RsvpStatus.PENDING,
                ),
            )
        }

        try {
            val allParticipants = listOfNotNull(event.ownerUid) + sharedWith.keys
            pushEventToFirestore(eventId, event, allParticipants)

            for ((uid, displayName) in sharedWith) {
                firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(uid)
                    .set(
                        mapOf(
                            "displayName" to displayName,
                            "rsvpStatus" to RsvpStatus.PENDING.name,
                        )
                    )
                    .await()
            }

            calendarEventDao.updatePendingSync(eventId, false)
        } catch (_: Exception) {
            // Stays pendingSync = true, will be retried by pushPendingEvents()
        }
        return eventId
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        authRepository.getCurrentUserUid() ?: throw NotSignedInException()
        val entity = event.toCustomEventEntity(event.id).copy(pendingSync = true)
        calendarEventDao.upsertEvent(entity)

        try {
            pushEventUpdateToFirestore(event.id, event)
            calendarEventDao.updatePendingSync(event.id, false)
        } catch (_: Exception) {
            // Stays pendingSync = true, will be retried by pushPendingEvents()
        }
    }

    override suspend fun addAttendees(eventId: String, sharedWith: Map<String, String>) {
        authRepository.getCurrentUserUid() ?: throw NotSignedInException()
        if (sharedWith.isEmpty()) return

        for ((uid, displayName) in sharedWith) {
            eventAttendeeDao.upsertAttendee(
                EventAttendeeEntity(
                    eventId = eventId,
                    userId = uid,
                    displayName = displayName,
                    rsvpStatus = RsvpStatus.PENDING,
                ),
            )
        }

        try {
            // arrayUnion is idempotent — re-adding an existing UID is a no-op,
            // safe to call without first reading the participants array.
            firestore.collection("events").document(eventId).update(
                "participants",
                FieldValue.arrayUnion(*sharedWith.keys.toTypedArray()),
            ).await()

            for ((uid, displayName) in sharedWith) {
                firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(uid)
                    .set(
                        mapOf(
                            "displayName" to displayName,
                            "rsvpStatus" to RsvpStatus.PENDING.name,
                        ),
                    )
                    .await()
            }
        } catch (_: Exception) {
            // Offline-first: Room has the attendees, Firestore push retries on reconnect
        }
    }

    override suspend fun removeAttendee(eventId: String, userId: String) {
        authRepository.getCurrentUserUid() ?: throw NotSignedInException()
        eventAttendeeDao.removeAttendee(eventId, userId)

        try {
            // arrayRemove is the symmetric counterpart to addAttendees' arrayUnion —
            // safe whether or not the uid is currently in the participants list.
            firestore.collection("events").document(eventId).update(
                "participants",
                FieldValue.arrayRemove(userId),
            ).await()

            firestore.collection("events")
                .document(eventId)
                .collection("attendees")
                .document(userId)
                .delete()
                .await()
        } catch (_: Exception) {
            // Offline-first: Room reflects the removal, Firestore reconciles on reconnect.
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        authRepository.getCurrentUserUid() ?: throw NotSignedInException()
        // Track deletion so the listener doesn't re-add it during the race window
        sharedEventSyncService.pendingDeletes.add(eventId)
        try {
            // Delete from Firestore first — listener will receive REMOVED change
            val attendeeDocs = firestore.collection("events")
                .document(eventId)
                .collection("attendees")
                .get()
                .await()
            for (doc in attendeeDocs.documents) {
                doc.reference.delete().await()
            }
            firestore.collection("events").document(eventId).delete().await()
        } catch (_: Exception) {
        }
        calendarEventDao.deleteEvent(eventId)
        sharedEventSyncService.pendingDeletes.remove(eventId)
    }

    override suspend fun rsvp(
        eventId: String,
        userId: String,
        displayName: String,
        status: RsvpStatus,
    ) {
        authRepository.getCurrentUserUid() ?: throw NotSignedInException()
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

    override suspend fun syncAttendees(eventId: String) {
        try {
            val docs = firestore.collection("events")
                .document(eventId)
                .collection("attendees")
                .get()
                .await()

            for (doc in docs.documents) {
                val rsvpName = doc.getString("rsvpStatus") ?: RsvpStatus.PENDING.name
                val status = try {
                    RsvpStatus.valueOf(rsvpName)
                } catch (_: Exception) {
                    RsvpStatus.PENDING
                }

                eventAttendeeDao.upsertAttendee(
                    EventAttendeeEntity(
                        eventId = eventId,
                        userId = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        rsvpStatus = status,
                    ),
                )
            }
        } catch (_: Exception) {
            // Offline — Room has whatever was last synced
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
            "ownerDisplayName" to (authRepository.getCurrentUserDisplayName() ?: ""),
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "startTime" to event.startTime.toEpochMilli(),
            "endTime" to event.endTime.toEpochMilli(),
            "categories" to event.categories.map { it.name },
            "participants" to participants,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "placeId" to event.placeId,
            "customLocation" to event.customLocation?.toFirestoreMap(),
            "type" to event.type.name,
            "courseLabel" to event.courseLabel,
            "isCompleted" to event.isCompleted,
        )
        firestore.collection("events").document(eventId).set(data).await()
    }

    /** Edits only the user-mutable fields. Critically excludes participants — that array
     *  is mutated independently by addAttendees/removeAttendee, and a `set(...)` write
     *  here would clobber the existing attendee membership. */
    private suspend fun pushEventUpdateToFirestore(eventId: String, event: CalendarEvent) {
        val data = mapOf(
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "startTime" to event.startTime.toEpochMilli(),
            "endTime" to event.endTime.toEpochMilli(),
            "categories" to event.categories.map { it.name },
            "placeId" to event.placeId,
            "customLocation" to event.customLocation?.toFirestoreMap(),
            "type" to event.type.name,
            "courseLabel" to event.courseLabel,
            "isCompleted" to event.isCompleted,
        )
        firestore.collection("events").document(eventId).update(data).await()
    }

    private fun com.ekhonavigator.core.model.SharedLocation.toFirestoreMap(): Map<String, Any> = mapOf(
        "title" to title,
        "latitude" to latitude,
        "longitude" to longitude,
    )

    override fun startSync(scope: CoroutineScope) {
        sharedEventSyncService.startListening(scope)
    }

    override fun stopSync() {
        sharedEventSyncService.stopListening()
    }

    override suspend fun onSignOut() {
        sharedEventSyncService.stopAndClearUserData()
    }
}
