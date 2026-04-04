package com.ekhonavigator.core.model

/**
 * Source types for the Schedule tab's multi-select filter chips.
 * Each maps to one or more [EventSource] values and/or bookmark state.
 *
 * Colors are NOT stored here — they live in the UI layer via MaterialTheme.
 */
enum class ScheduleSourceType(val displayName: String) {
    /** Class schedule events imported via ICS. */
    SCHEDULE("Schedule"),

    /** User-created and shared/group events. */
    CUSTOM("Custom"),

    /** Campus events from the iCal feed (all, including non-bookmarked). */
    CAMPUS("Campus"),

    /** Campus events the user has explicitly bookmarked. */
    BOOKMARKED("Bookmarked"),
}
