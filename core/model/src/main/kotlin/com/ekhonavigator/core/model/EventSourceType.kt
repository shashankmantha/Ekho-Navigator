package com.ekhonavigator.core.model

enum class EventSourceType(val displayName: String) {
    /** Class schedule events imported via ICS. */
    SCHEDULE("Schedule"),

    /** User-created and shared/group events. */
    CUSTOM("Custom"),

    /** Campus events from the iCal feed (all, including non-bookmarked). */
    CAMPUS("Campus"),

    /** Campus events the user has explicitly bookmarked. */
    BOOKMARKED("Bookmarked"),
}
