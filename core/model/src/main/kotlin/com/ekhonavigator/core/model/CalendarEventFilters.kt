package com.ekhonavigator.core.model

/**
 * True if this event belongs to any of the [activeTypes].
 */
fun CalendarEvent.matchesSourceTypes(activeTypes: Set<EventSourceType>): Boolean {
    for (type in activeTypes) {
        when (type) {
            EventSourceType.CUSTOM -> {
                if (source == EventSource.USER_CREATED || source == EventSource.SHARED) return true
            }
            EventSourceType.CAMPUS -> {
                if (source == EventSource.ICAL_FEED) return true
            }
            EventSourceType.BOOKMARKED -> {
                if (source == EventSource.ICAL_FEED && isBookmarked) return true
            }
            EventSourceType.CANVAS -> {
                if (source == EventSource.CANVAS) return true
            }
        }
    }
    return false
}

/** Empty [selected] means no filter — every event passes. */
fun CalendarEvent.matchesCategories(selected: Set<EventCategory>): Boolean =
    selected.isEmpty() || categories.any { it in selected }
