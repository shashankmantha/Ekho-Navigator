package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.model.toEntity
import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.data.sync.DailyEventNotificationManager
import com.ekhonavigator.core.data.util.SyncResult
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.EventAttendeeDao
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.database.model.toDomainModel
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.network.ICalFeedDataSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val eventAttendeeDao: EventAttendeeDao,
    private val iCalFeedDataSource: ICalFeedDataSource,
    private val authRepository: AuthRepository,
    private val dailyEventNotificationManager: DailyEventNotificationManager,
    private val placeRepository: PlaceRepository,
) : CalendarRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Never-completing empty-map source used when signed out. A one-shot `flowOf`
     * would finish and cause `combine` to cancel the upstream events flow, so the
     * UI would stop updating after the first emission.
     */
    private val emptyRsvpMap = MutableStateFlow(emptyMap<String, RsvpStatus>()).asStateFlow()

    /**
     * My RSVP status per eventId. Evaluates UID at subscription time so flows started
     * before sign-in become populated as soon as something collects post-sign-in.
     */
    private val myRsvpByEventId: Flow<Map<String, RsvpStatus>>
        get() = authRepository.getCurrentUserUid()?.let { uid ->
            eventAttendeeDao.observeAllForUser(uid)
                .map { rows -> rows.associate { it.eventId to it.rsvpStatus } }
        } ?: emptyRsvpMap

    private fun List<CalendarEventEntity>.joinMyRsvp(
        rsvps: Map<String, RsvpStatus>,
    ): List<CalendarEvent> = map { it.toDomainModel(myRsvpStatus = rsvps[it.uid]) }

    private fun List<CalendarEvent>.excludeDeclined(): List<CalendarEvent> =
        filter { it.myRsvpStatus != RsvpStatus.NOT_GOING }

    override fun observeEvents(): Flow<List<CalendarEvent>> =
        combine(calendarEventDao.observeAllEvents(), myRsvpByEventId) { entities, rsvps ->
            entities.joinMyRsvp(rsvps).excludeDeclined()
        }

    override fun observeBookmarkedEvents(): Flow<List<CalendarEvent>> =
        combine(calendarEventDao.observeBookmarkedEvents(), myRsvpByEventId) { entities, rsvps ->
            entities.joinMyRsvp(rsvps).excludeDeclined()
        }

    override fun observeEventsByDateRange(
        start: Instant,
        end: Instant,
    ): Flow<List<CalendarEvent>> =
        combine(
            calendarEventDao.observeEventsByDateRange(start, end),
            myRsvpByEventId,
        ) { entities, rsvps ->
            entities.joinMyRsvp(rsvps).excludeDeclined()
        }

    override fun observeEventById(id: String): Flow<CalendarEvent?> =
        combine(calendarEventDao.observeEventById(id), myRsvpByEventId) { entity, rsvps ->
            entity?.toDomainModel(myRsvpStatus = rsvps[entity.uid])
        }

    override fun observePendingInvites(): Flow<List<CalendarEvent>> =
        combine(calendarEventDao.observeAllEvents(), myRsvpByEventId) { entities, rsvps ->
            val myUid = authRepository.getCurrentUserUid()
            val now = Instant.now()
            entities.joinMyRsvp(rsvps).filter {
                it.myRsvpStatus == RsvpStatus.PENDING &&
                        it.ownerUid != myUid &&
                        it.endTime > now
            }
        }

    override fun observeDeclinedInvites(): Flow<List<CalendarEvent>> =
        combine(calendarEventDao.observeAllEvents(), myRsvpByEventId) { entities, rsvps ->
            val myUid = authRepository.getCurrentUserUid()
            val now = Instant.now()
            entities.joinMyRsvp(rsvps).filter {
                it.myRsvpStatus == RsvpStatus.NOT_GOING &&
                        it.ownerUid != myUid &&
                        it.endTime > now
            }
        }

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

            val entities = networkEvents.map { networkEvent ->
                val existing = calendarEventDao.getEventById(networkEvent.uid)
                networkEvent.toEntity(
                    existingBookmark = existing?.isBookmarked ?: false,
                    syncedAt = syncTime,
                    placeId = placeRepository.resolveFromText(networkEvent.location),
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
