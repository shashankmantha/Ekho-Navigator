package com.ekhonavigator.core.model

enum class EventSourceType(val displayName: String) {
    /** Anything with type == EventType.ASSIGNMENT — Canvas-bridged or user-created. */
    ASSIGNMENT("Assignment"),

    /** User-created and shared/group events that aren't assignments. */
    CUSTOM("Custom"),

    /** Campus events from the iCal feed (all, including non-bookmarked). */
    CAMPUS("Campus"),

    /** Campus events the user has explicitly bookmarked. */
    BOOKMARKED("Bookmarked"),
}
