package com.ekhonavigator.core.data.sync

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.model.firestoreDocToEntity
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.model.EventSource
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val calendarEventDao: CalendarEventDao,
    private val authRepository: AuthRepository,
) {
    private var listenerRegistration: ListenerRegistration? = null
    private var initialSyncDone = false

    /** Event IDs currently being deleted — listener skips these to avoid race conditions. */
    val pendingDeletes = mutableSetOf<String>()

    fun startListening(scope: CoroutineScope) {
        val uid = authRepository.getCurrentUserUid() ?: return
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

                                val source = if (ownerUid == uid) EventSource.USER_CREATED else EventSource.SHARED
                                val entity = firestoreDocToEntity(doc, source) ?: continue
                                calendarEventDao.upsertEvent(entity)
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
}
