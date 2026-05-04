package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Per-event presentation overlay sourced from Canvas data — kept off the
 * [com.ekhonavigator.core.model.CalendarEvent] model so the calendar layer doesn't
 * grow a Canvas dependency, and so the same composable rendering plain calendar
 * events can opt in to richer Canvas chrome (course color, completion strikethrough)
 * when the data is present.
 *
 * Provided once at app root via [LocalAssignmentDecorator]; render sites pull what
 * they need by event id. When Canvas isn't connected the decorator is empty — every
 * lookup returns null/false and the caller's fallback (theme primary color, no
 * strikethrough) takes over.
 *
 * **Color slots** index into [CoursePaletteLight] / [CoursePaletteDark]; the
 * decorator stores the index rather than the resolved [Color] so the same instance
 * can answer correctly under both themes without re-keying on `isSystemInDarkTheme`.
 */
@Stable
data class AssignmentDecorator(
    /** Event id → palette slot. Absent for events without a course tag. */
    val courseColorSlotByEventId: Map<String, Int> = emptyMap(),
    /** Event ids that should render with strikethrough (submitted/graded/excused). */
    val completedEventIds: Set<String> = emptySet(),
    /**
     * Event id → owning Canvas course id. Same key set as
     * [courseColorSlotByEventId] in practice but kept separate so callers can
     * answer "does this event belong to course X?" without going through colors.
     */
    val courseIdByEventId: Map<String, String> = emptyMap(),
    /**
     * Family-key (e.g. "COMP-262") → palette slot. Drives color resolution for
     * events that aren't keyed by Canvas event id — specifically personal events
     * with a user-supplied `courseLabel`. Same slot mapping the Canvas course
     * list uses, so `COMP-262` reads the same color across personal events and
     * Canvas-bridged assignments.
     */
    val familyKeyToSlot: Map<String, Int> = emptyMap(),
) {
    @Composable
    @ReadOnlyComposable
    fun courseColorFor(eventId: String): Color? {
        val slot = courseColorSlotByEventId[eventId] ?: return null
        return coursePalette()[slot % coursePalette().size]
    }

    /**
     * Resolves a free-text course label (e.g. "COMP-262") to a palette color via
     * family-key lookup. Returns null when no Canvas course matches the family,
     * so callers can decide whether to fall back to a neutral chip color.
     */
    @Composable
    @ReadOnlyComposable
    fun courseColorForLabel(label: String?): Color? {
        if (label.isNullOrBlank()) return null
        val key = CourseColorAssigner.familyKey(label)
        val slot = familyKeyToSlot[key] ?: return null
        return coursePalette()[slot % coursePalette().size]
    }

    fun isCompleted(eventId: String): Boolean = eventId in completedEventIds

    /**
     * The course id this event belongs to, or null if none — useful for filter
     * pipelines that need to drop events whose course was unselected.
     */
    fun courseIdFor(eventId: String): String? = courseIdByEventId[eventId]

    companion object {
        val Empty = AssignmentDecorator()
    }
}

/**
 * Composable hook for resolving Canvas-derived presentation overlays. Defaults to
 * [AssignmentDecorator.Empty] so previews and tests work without a provider.
 */
val LocalAssignmentDecorator = compositionLocalOf { AssignmentDecorator.Empty }
