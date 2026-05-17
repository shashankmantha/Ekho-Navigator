package com.ekhonavigator.core.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Weekly recurrence — covers ~95% of class schedules at CSUCI (MWF 9am, T/Th 2pm).
 * Full RFC 5545 RRULE is overkill; this is the minimum shape that lets a stored
 * event row expand into a series in the observation window.
 *
 * The series anchor lives on the parent [CalendarEvent.startTime] (first occurrence).
 * Each weekly occurrence inherits the same hour/minute and duration.
 */
data class RecurrenceRule(
    val daysOfWeek: Set<DayOfWeek>,
    val endDate: LocalDate,
)
