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
import com.ekhonavigator.core.model.isPast
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
import java.time.ZoneId
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
    ): Flow<List<CalendarEvent>> {
        val zone = ZoneId.systemDefault()
        val rangeStartEpochDay = start.atZone(zone).toLocalDate().toEpochDay()
        return combine(
            calendarEventDao.observeEventsByDateRange(start, end),
            calendarEventDao.observeRecurringEventsInRange(rangeStartEpochDay, end),
            myRsvpByEventId,
        ) { rangeEntities, recurringEntities, rsvps ->
            // The seed row of a series shows up in BOTH queries when its anchor
            // date is inside the window — drop it from the date-range bucket
            // so expansion is the single source for recurring events.
            val nonRecurring = rangeEntities.filter { it.recurrenceDaysOfWeek == null }
            val expanded = recurringEntities.flatMap { expandSeries(it, start, end, zone) }
            (nonRecurring + expanded).joinMyRsvp(rsvps).excludeDeclined()
        }
    }

    override fun observeEventById(id: String): Flow<CalendarEvent?> {
        // Recurrence instances carry composite ids (`seedUid__epochDay`) so each
        // pill has a unique LazyColumn key — strip back to the seed for lookup.
        val seedId = id.substringBefore(RECURRENCE_INSTANCE_DELIMITER)
        return combine(calendarEventDao.observeEventById(seedId), myRsvpByEventId) { entity, rsvps ->
            entity?.toDomainModel(myRsvpStatus = rsvps[entity.uid])
        }
    }

    override fun observePendingInvites(includePast: Boolean): Flow<List<CalendarEvent>> =
        observeInvitesWithStatus(RsvpStatus.PENDING, includePast)

    override fun observeDeclinedInvites(includePast: Boolean): Flow<List<CalendarEvent>> =
        observeInvitesWithStatus(RsvpStatus.NOT_GOING, includePast)

    private fun observeInvitesWithStatus(
        status: RsvpStatus,
        includePast: Boolean,
    ): Flow<List<CalendarEvent>> =
        combine(calendarEventDao.observeAllEvents(), myRsvpByEventId) { entities, rsvps ->
            val myUid = authRepository.getCurrentUserUid()
            val now = Instant.now()
            entities.joinMyRsvp(rsvps).filter { event ->
                event.myRsvpStatus == status &&
                    event.ownerUid != myUid &&
                    (includePast || !event.isPast(now))
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
        // Wipe Room bookmarks first so anything toggled while signed-out (or
        // belonging to a previous account on this device) doesn't survive into
        // the freshly-signed-in session. Firestore is the source of truth.
        calendarEventDao.clearAllBookmarks()
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

private const val RECURRENCE_INSTANCE_DELIMITER = "__"

// Generates one in-memory entity per day-of-week occurrence inside the window.
// Composite uid keeps LazyColumn keys unique while observeEventById() strips
// the suffix to navigate back to the single stored series row.
private fun expandSeries(
    seed: CalendarEventEntity,
    rangeStart: Instant,
    rangeEnd: Instant,
    zone: ZoneId,
): List<CalendarEventEntity> {
    val daysCsv = seed.recurrenceDaysOfWeek ?: return listOf(seed)
    val endEpochDay = seed.recurrenceEndDateEpochDay ?: return listOf(seed)
    val days = daysCsv.split(",")
        .mapNotNull { runCatching { java.time.DayOfWeek.valueOf(it.trim()) }.getOrNull() }
        .toSet()
    if (days.isEmpty()) return emptyList()

    val seedZoned = seed.startTime.atZone(zone)
    val seedTime = seedZoned.toLocalTime()
    val durationMs = seed.endTime.toEpochMilli() - seed.startTime.toEpochMilli()
    val seriesStartDate = seedZoned.toLocalDate()
    val seriesEndDate = java.time.LocalDate.ofEpochDay(endEpochDay)
    val windowStartDate = rangeStart.atZone(zone).toLocalDate()
    val windowEndDate = rangeEnd.atZone(zone).toLocalDate()

    val firstDate = if (seriesStartDate.isAfter(windowStartDate)) seriesStartDate else windowStartDate
    val lastDate = if (seriesEndDate.isBefore(windowEndDate)) seriesEndDate else windowEndDate
    if (firstDate.isAfter(lastDate)) return emptyList()

    val instances = mutableListOf<CalendarEventEntity>()
    var date = firstDate
    while (!date.isAfter(lastDate)) {
        if (date.dayOfWeek in days) {
            val startInstant = date.atTime(seedTime).atZone(zone).toInstant()
            if (startInstant >= rangeStart && startInstant < rangeEnd) {
                instances += seed.copy(
                    uid = "${seed.uid}$RECURRENCE_INSTANCE_DELIMITER${date.toEpochDay()}",
                    startTime = startInstant,
                    endTime = Instant.ofEpochMilli(startInstant.toEpochMilli() + durationMs),
                )
            }
        }
        date = date.plusDays(1)
    }
    return instances
}
