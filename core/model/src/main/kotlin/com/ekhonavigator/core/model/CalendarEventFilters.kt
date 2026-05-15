package com.ekhonavigator.core.model

/**
 * True if this event belongs to any of the [activeTypes].
 */
fun CalendarEvent.matchesSourceTypes(activeTypes: Set<EventSourceType>): Boolean {
    for (filter in activeTypes) {
        when (filter) {
            // Type-based: catches Canvas-bridged and personal assignments under one chip.
            EventSourceType.ASSIGNMENT -> {
                if (type == EventType.ASSIGNMENT) return true
            }
            // Personal non-assignments — assignments live under ASSIGNMENT instead.
            EventSourceType.CUSTOM -> {
                if ((source == EventSource.USER_CREATED || source == EventSource.SHARED) &&
                    type != EventType.ASSIGNMENT
                ) return true
            }
            EventSourceType.CAMPUS -> {
                if (source == EventSource.ICAL_FEED) return true
            }
            EventSourceType.BOOKMARKED -> {
                if (source == EventSource.ICAL_FEED && isBookmarked) return true
            }
        }
    }
    return false
}

/** Empty [selected] means no filter — every event passes. */
fun CalendarEvent.matchesCategories(selected: Set<EventCategory>): Boolean =
    selected.isEmpty() || categories.any { it in selected }
