package com.ekhonavigator.core.model

enum class EventCategory(val displayName: String, val color: Long) {
    CLASS("Class", 0xFFC8102E),
    EVENT("Event", 0xFF1565C0),
    SOCIAL("Social", 0xFF6A1B9A),
    DEADLINE("Deadline", 0xFFE65100),
    GENERAL("General", 0xFF607D8B);

    companion object {
        fun fromICalCategory(category: String): EventCategory =
            when (category.uppercase().trim()) {
                "CLASS", "LECTURE", "LAB", "COURSE" -> CLASS
                "EVENT", "MEETING", "WORKSHOP", "SEMINAR", "CONFERENCE" -> EVENT
                "SOCIAL", "CLUB", "RECREATION", "ATHLETICS" -> SOCIAL
                "DEADLINE", "DUE", "EXAM", "QUIZ", "ASSIGNMENT" -> DEADLINE
                else -> GENERAL
            }
    }
}
