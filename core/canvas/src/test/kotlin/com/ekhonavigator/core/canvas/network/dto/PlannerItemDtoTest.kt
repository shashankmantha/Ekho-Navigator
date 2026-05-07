package com.ekhonavigator.core.canvas.network.dto

import com.ekhonavigator.core.canvas.network.canvasJson
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerItemDtoTest {

    @Test
    fun `submissions object form deserializes to a SubmissionsDto with parsed flags`() {
        val json = """
            {
              "plannable_id": "100",
              "plannable_type": "assignment",
              "plannable_date": "2026-04-01T18:59:00Z",
              "html_url": "/x",
              "plannable": { "title": "Lab" },
              "submissions": { "submitted": true, "needs_grading": true }
            }
        """.trimIndent()

        val dto = canvasJson.decodeFromString<PlannerItemDto>(json)

        assertNotNull(dto.submissions)
        assertEquals(true, dto.submissions!!.submitted)
        assertEquals(true, dto.submissions!!.needsGrading)
    }

    @Test
    fun `submissions=false (Canvas's "no submission semantics" sentinel) deserializes to null`() {
        // Regression guard: Canvas sends `"submissions": false` for plannable types
        // that aren't submittable (announcements, calendar_events, planner_notes).
        // Was killing the whole /planner/items response with a JsonDecodingException
        // until the SubmissionsOrFalseSerializer landed.
        val json = """
            {
              "plannable_id": "200",
              "plannable_type": "announcement",
              "plannable_date": "2026-04-01T00:00:00Z",
              "html_url": "/y",
              "plannable": { "title": "Class canceled" },
              "submissions": false
            }
        """.trimIndent()

        val dto = canvasJson.decodeFromString<PlannerItemDto>(json)

        assertNull(dto.submissions)
    }

    @Test
    fun `submissions key absent entirely deserializes to null`() {
        val json = """
            {
              "plannable_id": "300",
              "plannable_type": "wiki_page",
              "plannable_date": "2026-04-01T00:00:00Z",
              "html_url": "/z",
              "plannable": { "title": "Page" }
            }
        """.trimIndent()

        val dto = canvasJson.decodeFromString<PlannerItemDto>(json)

        assertNull(dto.submissions)
    }

    @Test
    fun `mixed-shape array (the real Canvas response shape) parses every item`() {
        // The realistic case: Canvas returns assignments next to announcements next to
        // calendar_events. A single bad item used to torch the whole list parse.
        val json = """
            [
              {
                "plannable_id": "1",
                "plannable_type": "assignment",
                "plannable_date": "2026-04-01T18:59:00Z",
                "html_url": "/a/1",
                "plannable": { "title": "Lab", "due_at": "2026-04-01T18:59:00Z" },
                "submissions": { "submitted": false, "missing": true }
              },
              {
                "plannable_id": "2",
                "plannable_type": "announcement",
                "plannable_date": "2026-04-02T00:00:00Z",
                "html_url": "/n/2",
                "plannable": { "title": "Heads up" },
                "submissions": false
              },
              {
                "plannable_id": "3",
                "plannable_type": "calendar_event",
                "plannable_date": "2026-04-03T15:00:00Z",
                "html_url": "/c/3",
                "plannable": { "title": "Office hours" }
              }
            ]
        """.trimIndent()

        val items = canvasJson.decodeFromString(ListSerializer(PlannerItemDto.serializer()), json)

        assertEquals(3, items.size)
        assertNotNull(items[0].submissions)
        assertEquals(true, items[0].submissions!!.missing)
        assertNull(items[1].submissions)
        assertNull(items[2].submissions)
    }
}
