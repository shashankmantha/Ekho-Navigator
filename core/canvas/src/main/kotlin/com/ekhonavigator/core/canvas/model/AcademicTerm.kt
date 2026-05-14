package com.ekhonavigator.core.canvas.model

import java.time.LocalDate

// Text fallback for Canvas's term.end_at, which is frequently null at CSUCI
// even for past-term courses. Parses "Spring 2026" or "FA25"-style markers.
data class AcademicTerm(val season: Season, val year: Int) {

    enum class Season { SPRING, SUMMER, FALL, WINTER }

    // Wide windows cover registration through grade-post without bleeding into
    // the next term — strict CSUCI dates aren't needed for "is this current?"
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

// Accepts "Spring 2026", "FA25", and embedded variants. Null = "term unknown" —
// callers should usually include the course rather than hide it incorrectly.
object TermNameParser {

    private val LONG_FORM = Regex(
        "(spring|summer|fall|autumn|winter)\\s*(\\d{4})",
        RegexOption.IGNORE_CASE,
    )

    // Word-boundary so "SPACE 26" doesn't match "SP". Long form catches 4-digit years.
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
