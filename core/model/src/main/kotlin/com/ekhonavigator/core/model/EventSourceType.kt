package com.ekhonavigator.core.model

enum class EventSourceType(val displayName: String) {
    /** Assignments and events surfaced from the user's connected Canvas account. */
    CANVAS("Canvas"),

    /** User-created and shared/group events. */
    CUSTOM("Custom"),

    /** Campus events from the iCal feed (all, including non-bookmarked). */
    CAMPUS("Campus"),

    /** Campus events the user has explicitly bookmarked. */
    BOOKMARKED("Bookmarked"),
}
