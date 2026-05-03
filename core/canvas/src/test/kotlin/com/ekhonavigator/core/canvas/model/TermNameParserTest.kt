package com.ekhonavigator.core.canvas.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TermNameParserTest {

    @Test
    fun `long form Spring 2026 parses`() {
        val term = TermNameParser.parse("Spring 2026")
        assertEquals(AcademicTerm(AcademicTerm.Season.SPRING, 2026), term)
    }

    @Test
    fun `long form Fall 2025 parses case-insensitively`() {
        assertEquals(
            AcademicTerm(AcademicTerm.Season.FALL, 2025),
            TermNameParser.parse("fall 2025"),
        )
    }

    @Test
    fun `long form embedded inside larger string still parses`() {
        // CSUCI sometimes appends "Term" or section info — parser must reach in.
        assertEquals(
            AcademicTerm(AcademicTerm.Season.SPRING, 2026),
            TermNameParser.parse("Spring 2026 Term"),
        )
    }

    @Test
    fun `short form FA25 parses`() {
        assertEquals(
            AcademicTerm(AcademicTerm.Season.FALL, 2025),
            TermNameParser.parse("FA25"),
        )
    }

    @Test
    fun `short form embedded as course-name prefix parses`() {
        // Real-world example from a CSUCI advising course's `name` field.
        assertEquals(
            AcademicTerm(AcademicTerm.Season.FALL, 2025),
            TermNameParser.parse("FA25 - Advising Resources for Transfer Students"),
        )
    }

    @Test
    fun `SP26 short form parses`() {
        assertEquals(
            AcademicTerm(AcademicTerm.Season.SPRING, 2026),
            TermNameParser.parse("SP26"),
        )
    }

    @Test
    fun `null and blank input return null`() {
        assertNull(TermNameParser.parse(null))
        assertNull(TermNameParser.parse(""))
        assertNull(TermNameParser.parse("   "))
    }

    @Test
    fun `unrecognized text returns null — caller treats as unknown`() {
        // No semester marker → caller must decide (currently: include the course).
        assertNull(TermNameParser.parse("Default Term"))
        assertNull(TermNameParser.parse("Manually Created"))
    }

    @Test
    fun `bare 'SP' inside an unrelated word does not falsely match`() {
        // Word boundary on the regex prevents "SPACE 26" from being parsed as SP26.
        assertNull(TermNameParser.parse("SPACE 26 Workshop"))
    }

    @Test
    fun `Spring is current on a May date in the same year`() {
        val today = LocalDate.of(2026, 5, 3)
        assertTrue(AcademicTerm(AcademicTerm.Season.SPRING, 2026).isCurrent(today))
    }

    @Test
    fun `Fall 2025 is past on a May 2026 date`() {
        // The actual production case — FA25 must read as past in May 2026.
        val today = LocalDate.of(2026, 5, 3)
        assertFalse(AcademicTerm(AcademicTerm.Season.FALL, 2025).isCurrent(today))
    }

    @Test
    fun `Spring 2026 is past on a September 2026 date`() {
        val today = LocalDate.of(2026, 9, 1)
        assertFalse(AcademicTerm(AcademicTerm.Season.SPRING, 2026).isCurrent(today))
    }

    @Test
    fun `Fall is current on a November date in the same year`() {
        val today = LocalDate.of(2026, 11, 15)
        assertTrue(AcademicTerm(AcademicTerm.Season.FALL, 2026).isCurrent(today))
    }
}
