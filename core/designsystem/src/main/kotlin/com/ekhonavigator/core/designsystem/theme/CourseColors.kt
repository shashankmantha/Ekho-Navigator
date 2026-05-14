package com.ekhonavigator.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

/**
 * 6-color course rotation, redrawn for the CSUCI Channel Islands palette.
 *
 * All six slots live in the cyan→blue→purple→green-teal arc (`145°–290°`),
 * deliberately off the warm wedge (`340°–40°`) and yellow-green wedge (`70°–120°`)
 * claimed by the foundation roles (Clay 14°, Cardinal 345°, Horizon 25°, Sage 95°).
 * This gives courses their own coherent zone of the wheel with zero foundation
 * collisions. Order is locked — assignment uses sort-position indexing, so reordering
 * would shuffle every existing user's course colors.
 */
internal val CoursePaletteLight: List<Color> = listOf(
    Color(0xFF28556B), // 1 — Islands Blue   200° (CSUCI brand)
    Color(0xFFA07AB0), // 2 — Charoite       290° (CSUCI brand)
    Color(0xFF8C7325), // 3 — Citrine         46° — replaces Sycamore (145°),
                       //                            too close to Sage CUSTOM (95°).
                       //                            Darkened from #A88B2C to clear
                       //                            AA contrast against white text
                       //                            once past-event fade is applied.
    Color(0xFF4D6FB8), // 4 — Mariner        220°
    Color(0xFF3D8C8A), // 5 — Tidepool       178°
    Color(0xFF5C4A78), // 6 — Plum           265° — shifted from 280° to widen the
                       //                            10° gap to Charoite (290°). Now
                       //                            25° apart; reads as violet vs
                       //                            lavender instead of two purples.
)

internal val CoursePaletteDark: List<Color> = listOf(
    Color(0xFF7AA8C0), // 1 — Islands Blue   200°
    Color(0xFFC4A8D0), // 2 — Charoite       290°
    Color(0xFFB8965A), // 3 — Citrine         46° — kept darker than the rest of
                       //                            the dark palette so white text
                       //                            reads on top; yellow is bright
                       //                            no matter what.
    Color(0xFF8FA8DD), // 4 — Mariner        220°
    Color(0xFF7DBFBD), // 5 — Tidepool       178°
    Color(0xFF998BB5), // 6 — Plum           265°
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
