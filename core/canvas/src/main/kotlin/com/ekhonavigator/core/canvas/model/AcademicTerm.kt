package com.ekhonavigator.core.canvas.model

import java.time.LocalDate

/**
 * Parsed academic term derived from a Canvas `term.name` (e.g. "Spring 2026") or
 * a course `name` prefix (e.g. "FA25 - Advising Resources..."). Canvas's
 * `term.end_at` is the authoritative source when present, but at CSUCI it's
 * frequently null even for past-term courses — Canvas itself shows them as
 * active because there's no end timestamp. This parser is the fallback that
 * answers "is this course in the current semester?" purely from text.
 */
data class AcademicTerm(val season: Season, val year: Int) {

    enum class Season { SPRING, SUMMER, FALL, WINTER }

    /**
     * Inclusive calendar bounds of the term. CSUCI specifics aren't strict — the
     * windows are wide enough to cover the typical semester (registration through
     * grade-post) without bleeding into the next, so a simple `today in bounds`
     * check produces the right answer for normal cases.
     */
    private fun bounds(): Pair<LocalDate, LocalDate> = when (season) {
        Season.SPRING -> LocalDate.of(year, 1, 1) to LocalDate.of(year, 5, 31)
        Season.SUMMER -> LocalDate.of(year, 6, 1) to LocalDate.of(year, 8, 14)
        Season.FALL -> LocalDate.of(year, 8, 15) to LocalDate.of(year, 12, 31)
        Season.WINTER -> LocalDate.of(year, 12, 15) to LocalDate.of(year + 1, 1, 31)
    }

    fun isCurrent(today: LocalDate): Boolean {
        val (start, end) = bounds()
        return !today.isBefore(start) && !today.isAfter(end)
    }
}

/**
 * Best-effort parser for term markers embedded in Canvas-supplied strings.
 *
 * Recognized formats:
 * - Long: "Spring 2026", "Fall 2025", "Summer 2024" — case-insensitive, optional trailing words
 * - Short: "SP26", "FA25", "SU24", "WI25" — case-insensitive, 2-digit year (assumed 20xx)
 * - Embedded: "FA25 - Advising Resources...", "Spring 2026 Term"
 *
 * Returns null when no term marker is present (treat caller as "term unknown" —
 * usually safer to include the course than to hide it incorrectly).
 */
object TermNameParser {

    private val LONG_FORM = Regex(
        "(spring|summer|fall|autumn|winter)\\s*(\\d{4})",
        RegexOption.IGNORE_CASE,
    )

    // Word-boundary on the left so "SPACE 26" doesn't match the "SP" prefix.
    // Two-digit year only — four-digit years are caught by LONG_FORM.
    private val SHORT_FORM = Regex(
        "\\b(sp|su|fa|fall|wi)(\\d{2})\\b",
        RegexOption.IGNORE_CASE,
    )

    fun parse(text: String?): AcademicTerm? {
        if (text.isNullOrBlank()) return null

        LONG_FORM.find(text)?.let { match ->
            val season = parseLongSeason(match.groupValues[1]) ?: return@let
            val year = match.groupValues[2].toIntOrNull() ?: return@let
            return AcademicTerm(season, year)
        }

        SHORT_FORM.find(text)?.let { match ->
            val season = parseShortSeason(match.groupValues[1]) ?: return@let
            val twoDigitYear = match.groupValues[2].toIntOrNull() ?: return@let
            return AcademicTerm(season, 2000 + twoDigitYear)
        }

        return null
    }

    private fun parseLongSeason(s: String): AcademicTerm.Season? = when (s.lowercase()) {
        "spring" -> AcademicTerm.Season.SPRING
        "summer" -> AcademicTerm.Season.SUMMER
        "fall", "autumn" -> AcademicTerm.Season.FALL
        "winter" -> AcademicTerm.Season.WINTER
        else -> null
    }

    private fun parseShortSeason(s: String): AcademicTerm.Season? = when (s.lowercase()) {
        "sp" -> AcademicTerm.Season.SPRING
        "su" -> AcademicTerm.Season.SUMMER
        "fa", "fall" -> AcademicTerm.Season.FALL
        "wi" -> AcademicTerm.Season.WINTER
        else -> null
    }
}
