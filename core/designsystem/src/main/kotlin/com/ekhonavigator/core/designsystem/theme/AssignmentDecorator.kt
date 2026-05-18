package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Keeps Canvas-derived presentation overlays off the CalendarEvent model so
// the calendar layer doesn't grow a Canvas dependency. Stores palette slot
// indices (not Colors) so the same instance works in both themes.
@Stable
data class AssignmentDecorator(
    val courseColorSlotByEventId: Map<String, Int> = emptyMap(),
    val completedEventIds: Set<String> = emptySet(),
    // Same keys as courseColorSlotByEventId, but split out so callers can ask
    // "does this event belong to course X?" without going through colors.
    val courseIdByEventId: Map<String, String> = emptyMap(),
    // Drives color resolution for personal events tagged with a user-typed
    // courseLabel — same mapping the Canvas course list uses.
    val familyKeyToSlot: Map<String, Int> = emptyMap(),
    // Lets personal events with a courseLabel resolve to a Canvas course
    // for tap-to-detail nav. Sibling sections collapse to one courseId.
    val familyKeyToCourseId: Map<String, String> = emptyMap(),
    // User-picked custom hex per family-key. Wins over the palette slot when
    // both exist. Empty until the HSV picker ships.
    val familyKeyToCustomHex: Map<String, String> = emptyMap(),
    // Same custom-hex map but keyed by Canvas event id, for the planner pills.
    val courseColorHexByEventId: Map<String, String> = emptyMap(),
) {
    @Composable
    @ReadOnlyComposable
    fun courseColorFor(eventId: String): Color? {
        courseColorHexByEventId[eventId]?.let { return parseHexColor(it) }
        val slot = courseColorSlotByEventId[eventId] ?: return null
        return coursePalette()[slot % coursePalette().size]
    }

    // Null when no Canvas course matches — caller falls back to a neutral chip color.
    @Composable
    @ReadOnlyComposable
    fun courseColorForLabel(label: String?): Color? {
        if (label.isNullOrBlank()) return null
        val key = CourseColorAssigner.familyKey(label)
        familyKeyToCustomHex[key]?.let { return parseHexColor(it) }
        val slot = familyKeyToSlot[key] ?: return null
        return coursePalette()[slot % coursePalette().size]
    }

    fun isCompleted(eventId: String): Boolean = eventId in completedEventIds

    fun courseIdFor(eventId: String): String? = courseIdByEventId[eventId]

    fun courseIdForLabel(label: String?): String? {
        if (label.isNullOrBlank()) return null
        return familyKeyToCourseId[CourseColorAssigner.familyKey(label)]
    }

    companion object {
        val Empty = AssignmentDecorator()
    }
}

// Defaults to Empty so previews and tests work without a provider.
val LocalAssignmentDecorator = compositionLocalOf { AssignmentDecorator.Empty }

// Accepts "#RRGGBB" or "RRGGBB"; falls back to neutral grey on malformed input
// so a corrupted Firestore value never crashes the calendar.
private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#").take(6)
    val rgb = cleaned.toLongOrNull(16) ?: return Color(0xFF888888)
    return Color(0xFF000000L or rgb)
}
