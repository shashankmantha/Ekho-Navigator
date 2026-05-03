package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import com.ekhonavigator.core.model.EventSource
import com.ekhonavigator.core.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PlannerToCalendarMapperTest {

    @Test
    fun `assignment with due_at projects as ASSIGNMENT type starting at due-time`() {
        val due = Instant.parse("2026-04-01T18:59:00Z")
        val entity = entity(plannableType = "assignment", dueAt = due)

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals(EventType.ASSIGNMENT, calendarEvent.type)
        assertEquals(EventSource.CANVAS, calendarEvent.source)
        assertEquals(due, calendarEvent.startTime)
        assertEquals(due, calendarEvent.endTime)
        assertEquals(due, calendarEvent.dueAt)
    }

    @Test
    fun `assignment without due_at falls back to plannable_date`() {
        val plannable = Instant.parse("2026-04-15T12:00:00Z")
        val entity = entity(plannableType = "assignment", dueAt = null, plannableDate = plannable)

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals(plannable, calendarEvent.startTime)
        assertNull(calendarEvent.dueAt)
    }

    @Test
    fun `quiz projects as ASSIGNMENT — quizzes are due-date-driven like assignments`() {
        val entity = entity(plannableType = "quiz", dueAt = Instant.parse("2026-04-10T23:59:00Z"))

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals(EventType.ASSIGNMENT, calendarEvent.type)
    }

    @Test
    fun `canvas calendar_event projects as EVENT type at plannable_date`() {
        val plannable = Instant.parse("2026-04-20T15:00:00Z")
        val entity = entity(plannableType = "calendar_event", plannableDate = plannable, dueAt = null)

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals(EventType.EVENT, calendarEvent.type)
        assertEquals(plannable, calendarEvent.startTime)
    }

    @Test
    fun `announcement, discussion, planner_note, wiki_page, and unknown all skip the calendar`() {
        val skipped = listOf("announcement", "discussion_topic", "planner_note", "wiki_page", "frobulator")

        for (kind in skipped) {
            val entity = entity(plannableType = kind)
            assertNull("Expected $kind to skip the calendar surface", entity.toCalendarEventOrNull())
        }
    }

    @Test
    fun `mapped row carries externalSourceType and externalSourceId for window-prune lookup`() {
        val entity = entity(plannableType = "assignment", plannableId = "12345")

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals(CANVAS_PLANNER_ITEM_SOURCE, calendarEvent.externalSourceType)
        assertEquals("assignment_12345", calendarEvent.externalSourceId)
        assertEquals("assignment_12345", calendarEvent.uid)
    }

    @Test
    fun `mapped row uses contextName as ownerDisplayName for course attribution`() {
        val entity = entity(
            plannableType = "assignment",
            contextName = "COMP-362 Sec 001 - Operating Systems",
        )

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals("COMP-362 Sec 001 - Operating Systems", calendarEvent.ownerDisplayName)
        assertEquals("COMP-362 Sec 001 - Operating Systems", calendarEvent.organization)
    }

    @Test
    fun `mapped row holds the raw relative htmlUrl — domain prefix is the renderer's job`() {
        val entity = entity(
            plannableType = "assignment",
            htmlUrl = "/courses/33959/assignments/768813",
        )

        val calendarEvent = entity.toCalendarEventOrNull()!!

        assertEquals("/courses/33959/assignments/768813", calendarEvent.url)
    }

    private fun entity(
        plannableType: String,
        plannableId: String = "1",
        dueAt: Instant? = null,
        plannableDate: Instant = Instant.parse("2026-04-01T00:00:00Z"),
        contextName: String? = null,
        htmlUrl: String = "/x",
    ) = CanvasPlannerItemEntity(
        id = "${plannableType}_$plannableId",
        plannableType = plannableType,
        plannableId = plannableId,
        courseId = null,
        title = "$plannableType title",
        contextName = contextName,
        contextImage = null,
        plannableDate = plannableDate,
        dueAt = dueAt,
        pointsPossible = null,
        htmlUrl = htmlUrl,
        newActivity = false,
        submitted = false,
        late = false,
        missing = false,
        graded = false,
        needsGrading = false,
        hasFeedback = false,
        excused = false,
        lastSyncedAt = Instant.EPOCH,
    )
}
