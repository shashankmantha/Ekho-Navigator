package com.ekhonavigator.core.data.sync

import androidx.room.withTransaction
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.model.firestoreDocToEntity
import com.ekhonavigator.core.database.EkhoDatabase
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import com.ekhonavigator.core.database.model.EventAttendeeEntity
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.RsvpStatus
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for Firestore events where the current user is a participant.
 * Writes incoming shared events to Room so they appear in the unified feed.
 *
 * Only events NOT owned by the current user are written as [EventSource.SHARED].
 * Owned events are already in Room as [EventSource.USER_CREATED] from creation.
 */
@Singleton
class SharedEventSyncService @Inject constructor(
    private val database: EkhoDatabase,
    private val calendarEventDao: CalendarEventDao,
    private val eventAttendeeDao: EventAttendeeDao,
    private val authRepository: AuthRepository,
) {
    private var listenerRegistration: ListenerRegistration? = null
    private var initialSyncDone = false
    private var lastUid: String? = null

    /** Event IDs currently being deleted — listener skips these to avoid race conditions. */
    val pendingDeletes = mutableSetOf<String>()

    fun startListening(scope: CoroutineScope) {
        val uid = authRepository.getCurrentUserUid() ?: return

        lastUid = uid

        stopListening()
        initialSyncDone = false

        listenerRegistration = FirebaseFirestore.getInstance()
            .collection("events")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                scope.launch {
                    // On first snapshot, clean up stale custom/shared events
                    // that no longer exist in Firestore
                    if (!initialSyncDone) {
                        initialSyncDone = true
                        val remoteIds = snapshots.documents.map { it.id }.toSet()
                        val localCustom = calendarEventDao.observeMyEvents(uid).first()
                        val localShared = calendarEventDao.observeSharedEvents().first()
                        val allLocal = localCustom + localShared
                        for (event in allLocal) {
                            if (event.uid !in remoteIds) {
                                calendarEventDao.deleteEvent(event.uid)
                            }
                        }
                    }

                    for (change in snapshots.documentChanges) {
                        val doc = change.document
                        if (doc.id in pendingDeletes) continue

                        when (change.type) {
                            DocumentChange.Type.REMOVED -> {
                                calendarEventDao.deleteEvent(doc.id)
                            }

                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val ownerUid = doc.getString("ownerUid") ?: continue

                                if (ownerUid == uid) {
                                    val existing = calendarEventDao.getEventById(doc.id)
                                    if (existing != null) continue
                                }

                                val source =
                                    if (ownerUid == uid) EventSource.USER_CREATED else EventSource.SHARED
                                val entity = firestoreDocToEntity(doc, source) ?: continue

                                // Batch the event + optimistic PENDING attendee in one transaction
                                // so Room's invalidation tracker emits once. Otherwise the combined
                                // flow sees the event first (without an RSVP row), renders a plain
                                // blue pill, then ghost-styles on the next emission — visible flicker
                                // until the user triggers a recomposition by navigating back.
                                database.withTransaction {
                                    calendarEventDao.upsertEvent(entity)
                                    if (source == EventSource.SHARED) {
                                        val existingRsvp = eventAttendeeDao.getAttendee(doc.id, uid)
                                        if (existingRsvp == null) {
                                            eventAttendeeDao.upsertAttendee(
                                                EventAttendeeEntity(
                                                    eventId = doc.id,
                                                    userId = uid,
                                                    displayName = authRepository.getCurrentUserDisplayName()
                                                        ?: "",
                                                    rsvpStatus = RsvpStatus.PENDING,
                                                ),
                                            )
                                        }
                                    }
                                }

                                // Confirm true status from Firestore (e.g. the user already RSVP'd
                                // on another device).
                                if (source == EventSource.SHARED) {
                                    syncMyAttendee(eventId = doc.id, userId = uid)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private suspend fun syncMyAttendee(eventId: String, userId: String) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("attendees")
                .document(userId)
                .get()
                .await()
            if (!doc.exists()) return

            val statusName = doc.getString("rsvpStatus") ?: RsvpStatus.PENDING.name
            val status = runCatching { RsvpStatus.valueOf(statusName) }
                .getOrDefault(RsvpStatus.PENDING)

            eventAttendeeDao.upsertAttendee(
                EventAttendeeEntity(
                    eventId = eventId,
                    userId = userId,
                    displayName = doc.getString("displayName") ?: "",
                    rsvpStatus = status,
                ),
            )
        } catch (_: Exception) {
            // Offline — Room keeps whatever was last synced; event will still render
            // with null myRsvpStatus until we recover.
        }
    }

    /** Stop listening and clear all user-specific events from Room. Call on sign-out. */
    suspend fun stopAndClearUserData() {
        stopListening()
        lastUid = null
        calendarEventDao.deleteAllUserEvents()
    }
}
