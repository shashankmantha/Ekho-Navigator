package com.ekhonavigator.core.network.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * One parsed VEVENT from a user-picked .ics file, in a shape that hasn't yet
 * committed to becoming a [com.ekhonavigator.core.model.CalendarEvent]. The
 * preview screen lets the user accept/reject each entry; survivors are mapped
 * to domain events at commit time.
 *
 * `sourceUid` is the ICS UID — kept so a re-import of the same file doesn't
 * silently create duplicates downstream.
 */
data class StagedImportedEvent(
    val sourceUid: String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: Instant,
    val endTime: Instant,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val recurrenceEndDate: LocalDate? = null,
    val inferredCourseLabel: String? = null,
)
