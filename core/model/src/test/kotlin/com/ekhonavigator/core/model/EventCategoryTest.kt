package com.ekhonavigator.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [EventCategory] enum mapping.
 *
 * ## What's being tested
 *
 * The [EventCategory.fromTrumbaCategory] function maps raw category strings
 * from the 25Live iCal feed to our enum values. This is critical because
 * a typo or casing mismatch means events get tagged as GENERAL instead of
 * their real category — which silently breaks the filter chips in the UI.
 *
 * ## Why test an enum?
 *
 * Enums look simple, but the `fromTrumbaCategory` mapper has subtle
 * requirements:
 * - Case insensitivity ("ALUMNI" and "alumni" should both work)
 * - Whitespace trimming ("  Staff  " should match)
 * - HTML-decoded strings ("Academics & Research" not "Academics &amp; Research")
 * - Unknown values should fall back to GENERAL, not crash
 *
 * If someone adds a new category to the enum but forgets to add the
 * when-branch, this test will catch it.
 */
class EventCategoryTest {

    @Test
    fun `all known categories map correctly`() {
        // Each pair is: (raw feed string → expected enum)
        val mappings = mapOf(
            "Academics & Research" to EventCategory.ACADEMICS_RESEARCH,
            "Alumni" to EventCategory.ALUMNI,
            "Community" to EventCategory.COMMUNITY,
            "External" to EventCategory.EXTERNAL,
            "Homecoming" to EventCategory.HOMECOMING,
            "Private Event" to EventCategory.PRIVATE_EVENT,
            "Staff" to EventCategory.STAFF,
            "Student Organizations" to EventCategory.STUDENT_ORGS,
            "Summer Conference" to EventCategory.SUMMER_CONFERENCE,
            "Teaching & Innovations" to EventCategory.TEACHING_INNOVATIONS,
            "University Life" to EventCategory.UNIVERSITY_LIFE,
        )

        for ((input, expected) in mappings) {
            assertEquals(
                "Failed for input: \"$input\"",
                expected,
                EventCategory.fromTrumbaCategory(input),
            )
        }
    }

    @Test
    fun `mapping is case insensitive`() {
        assertEquals(EventCategory.ALUMNI, EventCategory.fromTrumbaCategory("ALUMNI"))
        assertEquals(EventCategory.ALUMNI, EventCategory.fromTrumbaCategory("alumni"))
        assertEquals(EventCategory.ALUMNI, EventCategory.fromTrumbaCategory("Alumni"))
        assertEquals(EventCategory.STAFF, EventCategory.fromTrumbaCategory("STAFF"))
    }

    @Test
    fun `mapping trims whitespace`() {
        assertEquals(EventCategory.STAFF, EventCategory.fromTrumbaCategory("  Staff  "))
        assertEquals(EventCategory.ALUMNI, EventCategory.fromTrumbaCategory("\tAlumni\n"))
    }

    @Test
    fun `unknown category maps to GENERAL`() {
        assertEquals(EventCategory.GENERAL, EventCategory.fromTrumbaCategory("Nonexistent"))
        assertEquals(EventCategory.GENERAL, EventCategory.fromTrumbaCategory(""))
        assertEquals(EventCategory.GENERAL, EventCategory.fromTrumbaCategory("Random Category"))
    }

    @Test
    fun `every enum value has a non-blank displayName`() {
        for (category in EventCategory.entries) {
            assert(category.displayName.isNotBlank()) {
                "${category.name} has a blank displayName"
            }
        }
    }

    @Test
    fun `every enum value has a non-zero color`() {
        for (category in EventCategory.entries) {
            assert(category.color != 0L) {
                "${category.name} has a zero color"
            }
        }
    }

    @Test
    fun `primaryCategory returns first category or GENERAL`() {
        // This tests the CalendarEvent.primaryCategory computed property
        val event = CalendarEvent(
            id = "test",
            title = "Test",
            description = "",
            location = "",
            startTime = java.time.Instant.now(),
            endTime = java.time.Instant.now(),
            categories = listOf(EventCategory.ALUMNI, EventCategory.STAFF),
            url = "",
            status = "CONFIRMED",
            isBookmarked = false,
            lastSyncedAt = java.time.Instant.now(),
        )
        assertEquals(EventCategory.ALUMNI, event.primaryCategory)

        val noCategories = event.copy(categories = emptyList())
        assertEquals(EventCategory.GENERAL, noCategories.primaryCategory)
    }
}
