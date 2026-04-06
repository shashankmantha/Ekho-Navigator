package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.model.toEntity
import com.ekhonavigator.core.data.sync.DailyEventNotificationManager
import com.ekhonavigator.core.data.util.SyncResult
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.model.toDomainModel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.network.ICalFeedDataSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCalendarRepository @Inject constructor(
    private val calendarEventDao: CalendarEventDao,
    private val iCalFeedDataSource: ICalFeedDataSource,
    private val authRepository: AuthRepository,
    private val dailyEventNotificationManager: DailyEventNotificationManager,
) : CalendarRepository {

    private val firestore = FirebaseFirestore.getInstance()

    override fun observeEvents(): Flow<List<CalendarEvent>> =
        calendarEventDao.observeAllEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeBookmarkedEvents(): Flow<List<CalendarEvent>> =
        calendarEventDao.observeBookmarkedEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeEventsByDateRange(
        start: Instant,
        end: Instant,
    ): Flow<List<CalendarEvent>> =
        calendarEventDao.observeEventsByDateRange(start, end).map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeEventById(id: String): Flow<CalendarEvent?> =
        calendarEventDao.observeEventById(id).map { it?.toDomainModel() }

    override suspend fun toggleBookmark(eventId: String) {
        val event = calendarEventDao.getEventById(eventId) ?: return
        val nowBookmarked = !event.isBookmarked
        calendarEventDao.updateBookmark(eventId, nowBookmarked)

        if (nowBookmarked) {
            dailyEventNotificationManager.notifyEventBookmarked(eventId)
        }

        // Fire-and-forget sync to Firestore for push notification readiness
        val uid = authRepository.getCurrentUserUid() ?: return
        val safeDocId = URLEncoder.encode(eventId, "UTF-8")
        val docRef = firestore.collection("users").document(uid)
            .collection("bookmarkedEvents").document(safeDocId)

        try {
            if (nowBookmarked) {
                docRef.set(
                    mapOf(
                        "eventId" to eventId,
                        "title" to event.title,
                        "startTime" to event.startTime.toEpochMilli(),
                        "location" to event.location,
                        "bookmarkedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            } else {
                docRef.delete().await()
            }
        } catch (_: Exception) {
            // Offline-first: Room has the bookmark state, Firestore syncs when back online
        }
    }

    override suspend fun restoreBookmarks() {
        val uid = authRepository.getCurrentUserUid() ?: return
        try {
            val docs = firestore.collection("users").document(uid)
                .collection("bookmarkedEvents")
                .get()
                .await()

            for (doc in docs.documents) {
                val eventId = doc.getString("eventId") ?: URLDecoder.decode(doc.id, "UTF-8")
                calendarEventDao.updateBookmark(eventId, true)
            }
        } catch (_: Exception) {
            // Offline — bookmarks will restore on next successful fetch
        }
    }

    override suspend fun onSignOut() {
        calendarEventDao.clearAllBookmarks()
    }

    override suspend fun sync(feedUrl: String): SyncResult {
        return try {
            val networkEvents = iCalFeedDataSource.fetchEvents(feedUrl)
            val syncTime = Instant.now()

            // Preserve bookmark status for events that already exist locally
            val entities = networkEvents.map { networkEvent ->
                val existing = calendarEventDao.getEventById(networkEvent.uid)
                networkEvent.toEntity(
                    existingBookmark = existing?.isBookmarked ?: false,
                    syncedAt = syncTime,
                )
            }

            calendarEventDao.upsertEvents(entities)

            val activeUids = networkEvents.map { it.uid }.toSet()
            calendarEventDao.deleteICalEventsNotIn(activeUids.toList())

            cleanupStaleBookmarks(activeUids)

            SyncResult.Success(eventsUpdated = entities.size)
        } catch (e: Exception) {
            SyncResult.Error(
                message = e.message ?: "Unknown sync error",
                cause = e,
            )
        }
    }

    private suspend fun cleanupStaleBookmarks(activeUids: Set<String>) {
        val uid = authRepository.getCurrentUserUid() ?: return
        try {
            val docs = firestore.collection("users").document(uid)
                .collection("bookmarkedEvents")
                .get()
                .await()

            for (doc in docs.documents) {
                val eventId = doc.getString("eventId") ?: URLDecoder.decode(doc.id, "UTF-8")
                if (eventId !in activeUids) {
                    doc.reference.delete().await()
                }
            }
        } catch (_: Exception) {
            // Best-effort cleanup — will retry on next sync
        }
    }
}
