package com.ekhonavigator.core.model

/**
 * Primary visual axis for an event — drives color, icon, and due-date emphasis.
 * Orthogonal to [EventSource] (where the data came from). A user-created study task
 * and a Canvas assignment both render as [ASSIGNMENT] regardless of source.
 */
enum class EventType {
    EVENT,
    ASSIGNMENT,
    /** Recurring lecture/lab block. Carries a [RecurrenceRule] so the calendar
     *  expands one stored row into a weekly series until the term ends. */
    CLASS_MEETING,
}
