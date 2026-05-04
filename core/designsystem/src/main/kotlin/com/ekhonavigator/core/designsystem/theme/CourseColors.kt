package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

/**
 * 6-color rotation used to color-code course-tagged assignments on calendar surfaces.
 *
 * Light/dark variants are paired by index — `lightPalette[i]` and `darkPalette[i]`
 * always represent the same conceptual color slot. Index assignment is by
 * [CourseColorAssigner.assign], not a hash, so the same family always lands in the
 * same slot for a given active course set.
 *
 * Palette dropped from 8 → 6 slots in 2026-05 to fix a sage-vs-teal collision (CUSTOM
 * source chip and COMP-courses both reading as "green") and a latent garnet-vs-coral
 * collision (CANVAS source chip and any 15° course slot reading as "red"). Cuts:
 *   - CourseTeal (175°) — too close to sage Custom (140°)
 *   - CourseCoral (15°) — too close to garnet Canvas (5°)
 * Family clustering means a typical student carries 4-5 active families per term, so
 * 6 slots covers the realistic load. If users with 7+ cross-department course loads
 * report color collisions, revisit the size.
 */
// Slot order alternates cool/warm so adjacent family-key sort positions
// (e.g. ASL-101 in slot 0, BIO-110 in slot 1) get maximum hue separation.
internal val CoursePaletteLight: List<Color> = listOf(
    CourseSlateBlue,  // 210°
    CourseEarth,      //  30° — dark coffee brown, distinct from garnet via luminance
    CourseLavender,   // 250°
    CourseRose,       // 335° — magenta-pink, 30° from garnet
    CourseOlive,      //  75° — yellow-green, distinct from amber via hue lean
    CoursePlum,       // 290°
)

internal val CoursePaletteDark: List<Color> = listOf(
    CourseSlateBlueDark,
    CourseEarthDark,
    CourseLavenderDark,
    CourseRoseDark,
    CourseOliveDark,
    CoursePlumDark,
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
/**
 * Normalizes raw user input into the canonical course-code shape that
 * [CourseColorAssigner.familyKey] can extract from. Handles common copy/paste
 * and casing variations so `comp 262` and `Comp262` both round-trip into
 * `COMP-262`. Empty input returns null so callers can drop the field cleanly.
 */
fun normalizeCourseLabel(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    // Uppercase first; then collapse internal whitespace; then ensure a hyphen
    // sits between the leading letters and digits if missing.
    val upper = trimmed.uppercase()
    val noInnerSpaces = upper.replace(Regex("\\s+"), "")
    return Regex("^([A-Z]+)(\\d+.*)\$").find(noInnerSpaces)?.let { match ->
        "${match.groupValues[1]}-${match.groupValues[2]}"
    } ?: noInnerSpaces
}

object CourseColorAssigner {

    private val FAMILY_KEY_REGEX = Regex("^([A-Z]+-?\\d+)")

    /**
     * Builds a `courseId -> paletteIndex` map. Pass the full active course set
     * (favorites + non-favorites) so families assign before the user toggles
     * filters; otherwise indices would shift each time a course is hidden.
     */
    fun assign(courses: List<CourseColorInput>): Map<String, Int> {
        if (courses.isEmpty()) return emptyMap()

        return courses.associate { course ->
            course.id to familySlots(courses).getValue(familyKey(course.code))
        }
    }

    /**
     * Builds the `familyKey -> paletteIndex` map directly. Same sort-position
     * logic as [assign], but exposes the family-key map so callers without a
     * concrete course id (e.g. personal events tagged with a free-text course
     * label) can resolve a color through a shared lookup.
     */
    fun familySlots(courses: List<CourseColorInput>): Map<String, Int> =
        courses
            .map { familyKey(it.code) }
            .distinct()
            .sorted()
            .withIndex()
            .associate { (slot, family) -> family to slot }

    /** Extracts the family key (e.g. `COMP-262`) from a raw course code or
     *  free-text label. Falls back to the full input string if no leading
     *  letters+digits prefix is found. */
    fun familyKey(code: String): String =
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
