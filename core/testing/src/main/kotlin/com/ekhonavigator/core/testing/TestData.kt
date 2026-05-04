package com.ekhonavigator.core.testing

import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventCategory
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.model.SharedLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Factory for building [CalendarEvent] instances in tests.
 *
 * Every parameter has a sensible default so tests only need to specify
 * the fields they care about. This keeps tests focused on what they're
 * actually testing rather than boilerplate data setup.
 *
 * ## Usage
 *
 * ```kotlin
 * // Minimal — just needs an ID
 * val event = testCalendarEvent(id = "1")
 *
 * // Customised — override what matters for this test
 * val past = testCalendarEvent(
 *     id = "old",
 *     startTime = Instant.parse("2020-01-01T00:00:00Z"),
 * )
 * ```
 */
fun testCalendarEvent(
    id: String = "test-id",
    title: String = "Test Event",
    description: String = "A test event description",
    location: String = "Test Hall",
    startTime: Instant = LocalDate.now().plusDays(1)
        .atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant(),
    endTime: Instant = LocalDate.now().plusDays(1)
        .atTime(23, 59).atZone(ZoneId.of("America/Los_Angeles")).toInstant(),
    categories: List<EventCategory> = listOf(EventCategory.GENERAL),
    url: String = "",
    status: String = "CONFIRMED",
    isBookmarked: Boolean = false,
    lastSyncedAt: Instant = Instant.now(),
    source: EventSource = EventSource.ICAL_FEED,
    ownerUid: String? = null,
    ownerDisplayName: String = "",
    pendingSync: Boolean = false,
    myRsvpStatus: RsvpStatus? = null,
    eventName: String = "",
    organization: String = "",
    eventType: String = "",
    placeId: String? = null,
    externalSourceId: String? = null,
    externalSourceType: String? = null,
    dueAt: Instant? = null,
    customLocation: SharedLocation? = null,
    type: EventType = EventType.EVENT,
    courseLabel: String? = null,
): CalendarEvent = CalendarEvent(
    id = id,
    title = title,
    description = description,
    location = location,
    startTime = startTime,
    endTime = endTime,
    categories = categories,
    url = url,
    status = status,
    isBookmarked = isBookmarked,
    lastSyncedAt = lastSyncedAt,
    source = source,
    ownerUid = ownerUid,
    ownerDisplayName = ownerDisplayName,
    pendingSync = pendingSync,
    myRsvpStatus = myRsvpStatus,
    eventName = eventName,
    organization = organization,
    eventType = eventType,
    placeId = placeId,
    externalSourceId = externalSourceId,
    externalSourceType = externalSourceType,
    dueAt = dueAt,
    customLocation = customLocation,
    type = type,
    courseLabel = courseLabel,
)
