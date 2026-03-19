package com.ekhonavigator.core.model

/**
 * Event categories matching the CSUCI 25Live Publisher feed.
 * These come from the X-TRUMBA-CUSTOMFIELD;NAME="Categories" property.
 */
enum class EventCategory(val displayName: String, val color: Long) {
    ACADEMICS_RESEARCH("Academics & Research", 0xFF1565C0),
    ALUMNI("Alumni", 0xFF00897B),
    COMMUNITY("Community", 0xFF2E7D32),
    EXTERNAL("External", 0xFF78909C),
    HOMECOMING("Homecoming", 0xFFF9A825),
    PRIVATE_EVENT("Private Event", 0xFF6A1B9A),
    STAFF("Staff", 0xFF1E88E5),
    STUDENT_ORGS("Student Organizations", 0xFFAB47BC),
    SUMMER_CONFERENCE("Summer Conference", 0xFF00ACC1),
    TEACHING_INNOVATIONS("Teaching & Innovations", 0xFFE65100),
    UNIVERSITY_LIFE("University Life", 0xFFC62828),
    GENERAL("General", 0xFF607D8B);

    companion object {
        /**
         * Maps the raw category string from X-TRUMBA-CUSTOMFIELD;NAME="Categories"
         * to an [EventCategory]. Case-insensitive, trims whitespace.
         */
        fun fromTrumbaCategory(category: String): EventCategory =
            when (category.trim().lowercase()) {
                "academics & research" -> ACADEMICS_RESEARCH
                "alumni" -> ALUMNI
                "community" -> COMMUNITY
                "external" -> EXTERNAL
                "homecoming" -> HOMECOMING
                "private event" -> PRIVATE_EVENT
                "staff" -> STAFF
                "student organizations" -> STUDENT_ORGS
                "summer conference" -> SUMMER_CONFERENCE
                "teaching & innovations" -> TEACHING_INNOVATIONS
                "university life" -> UNIVERSITY_LIFE
                else -> GENERAL
            }
    }
}
