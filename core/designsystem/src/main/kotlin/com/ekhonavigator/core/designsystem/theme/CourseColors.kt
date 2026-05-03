package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

/**
 * 8-color rotation used to color-code course-tagged assignments on calendar surfaces.
 *
 * Light/dark variants are paired by index — `lightPalette[i]` and `darkPalette[i]`
 * always represent the same conceptual color slot. Index assignment is by
 * [CourseColorAssigner.assign], not a hash, so the same family always lands in the
 * same slot for a given active course set.
 */
// Slot order alternates cool/warm/cool/warm so adjacent family-key sort positions
// (e.g. ASL-101 in slot 0, BIO-110 in slot 1) get maximum hue separation.
internal val CoursePaletteLight: List<Color> = listOf(
    CourseSlateBlue,  // 210°
    CourseCoral,      //  15°
    CourseTeal,       // 175°
    CourseRose,       // 335°
    CourseEarth,      //  30° (dark, separates from coral via luminance)
    CoursePlum,       // 290°
    CourseOlive,      //  75°
    CourseLavender,   // 250°
)

internal val CoursePaletteDark: List<Color> = listOf(
    CourseSlateBlueDark,
    CourseCoralDark,
    CourseTealDark,
    CourseRoseDark,
    CourseEarthDark,
    CoursePlumDark,
    CourseOliveDark,
    CourseLavenderDark,
)

@Composable
@ReadOnlyComposable
fun coursePalette(): List<Color> =
    if (isSystemInDarkTheme()) CoursePaletteDark else CoursePaletteLight

/**
 * Assigns palette indices to courses by family-key sort position rather than hashing.
 *
 * **Algorithm** (locked):
 * 1. Extract a family key from each course's `code` via `^([A-Z]+-?\d+)`.
 *    `COMP-262 Sec 001`, `COMP-262 Sec 01L`, and `COMP-262` all collapse to `COMP-262`,
 *    so lab + lecture sections of the same course share a color automatically.
 *    Unparseable codes fall back to the full code string so they still get a slot.
 * 2. Collect the distinct set of family keys across the active course list, sort
 *    alphabetically.
 * 3. Each family's palette index is its sort position `mod 8`. Computed at render
 *    time; never persisted. Auto-rebalances when enrollments change.
 *
 * Trade-off: the same course family across semesters may shift slot. This is fine —
 * users mostly view one semester at a time and per-semester collision-freedom wins
 * over cross-semester color identity.
 */
object CourseColorAssigner {

    private val FAMILY_KEY_REGEX = Regex("^([A-Z]+-?\\d+)")

    /**
     * Builds a `courseId -> paletteIndex` map. Pass the full active course set
     * (favorites + non-favorites) so families assign before the user toggles
     * filters; otherwise indices would shift each time a course is hidden.
     */
    fun assign(courses: List<CourseColorInput>): Map<String, Int> {
        if (courses.isEmpty()) return emptyMap()

        // Family keys, sorted; build the slot-position lookup once.
        val familySlots: Map<String, Int> = courses
            .map { familyKey(it.code) }
            .distinct()
            .sorted()
            .withIndex()
            .associate { (slot, family) -> family to slot }

        return courses.associate { course ->
            course.id to (familySlots.getValue(familyKey(course.code)))
        }
    }

    private fun familyKey(code: String): String =
        FAMILY_KEY_REGEX.find(code)?.groupValues?.get(1) ?: code
}

/**
 * Minimal projection of a course needed for color assignment. Defined here (not as
 * `CanvasCourse`) so the design system stays Canvas-agnostic — user-added courses
 * (Phase 5.6) will feed in through the same shape.
 */
data class CourseColorInput(
    val id: String,
    val code: String,
)
