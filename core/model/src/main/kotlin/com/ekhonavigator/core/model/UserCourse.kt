package com.ekhonavigator.core.model

import java.time.Instant

/**
 * A course the user has on their profile — either typed manually or seeded
 * from a Canvas sync. The [familyKey] is the Firestore document id, so
 * "COMP-262 Sec 001" and "COMP-262 Sec 01L" both collapse to a single row
 * keyed by "COMP-262" and share a color across sections and semesters.
 */
data class UserCourse(
    val familyKey: String,
    val code: String,
    val displayName: String,
    val colorChoice: CourseColorChoice,
    val archived: Boolean = false,
    val createdAt: Instant = Instant.now(),
)

/**
 * Where the course's color comes from. Palette wins by default — slot indexes
 * resolve through the theme so light/dark adapt automatically. Custom is the
 * forward-compat slot for a future HSV picker; nothing writes it yet.
 */
sealed interface CourseColorChoice {
    data class Palette(val slot: Int) : CourseColorChoice
    data class Custom(val hex: String) : CourseColorChoice
}
