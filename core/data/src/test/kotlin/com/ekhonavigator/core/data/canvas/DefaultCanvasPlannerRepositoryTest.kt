package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerKind
import com.ekhonavigator.core.canvas.network.dto.PlannableDto
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import com.ekhonavigator.core.canvas.network.dto.SubmissionsDto
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DefaultCanvasPlannerRepositoryTest {

    private val api = FakeCanvasApi()
    private val provider = FakeCanvasApiProvider(api = api)
    private val dao = FakeCanvasPlannerItemDao()
    private val repo = DefaultCanvasPlannerRepository(provider, dao)

    private val windowStart = Instant.parse("2026-04-01T00:00:00Z")
    private val windowEnd = Instant.parse("2026-05-01T00:00:00Z")

    @Test
    fun `sync without a Canvas account fails with NoCanvasAccountException and leaves dao untouched`() = runTest {
        provider.api = null
        dao.seed(listOf(entity("kept", Instant.parse("2026-04-15T00:00:00Z"))))

        val result = repo.sync(windowStart, windowEnd)

        assertSame(NoCanvasAccountException, result.exceptionOrNull())
        assertEquals(listOf("kept"), dao.snapshot().map { it.id })
        assertTrue(api.plannerCalls.isEmpty())
    }

    @Test
    fun `sync upserts items returned by Canvas and prunes others within the window`() = runTest {
        dao.seed(
            listOf(
                entity(id = "assignment_stale", date = Instant.parse("2026-04-10T00:00:00Z")),
                entity(id = "assignment_outside", date = Instant.parse("2026-06-01T00:00:00Z")),
            ),
        )
        api.plannerItemsToReturn = listOf(
            dto(plannableId = "100", plannableType = "assignment", date = "2026-04-15T18:59:00Z"),
            dto(plannableId = "200", plannableType = "quiz", date = "2026-04-20T18:59:00Z"),
        )

        val result = repo.sync(windowStart, windowEnd)

        assertTrue(result.isSuccess)
        val ids = dao.snapshot().map { it.id }.toSet()
        // assignment_stale is in-window and not in response → pruned; assignment_outside is preserved.
        assertEquals(setOf("assignment_100", "quiz_200", "assignment_outside"), ids)
        // Window dates are passed to Canvas as ISO-8601 instants.
        assertEquals(1, api.plannerCalls.size)
        assertEquals("2026-04-01T00:00:00Z", api.plannerCalls.single().first)
    }

    @Test
    fun `mapper extracts due_at, points_possible, and submission status from canvas plannable shape`() = runTest {
        api.plannerItemsToReturn = listOf(
            PlannerItemDto(
                plannableId = "768813",
                plannableType = "assignment",
                courseId = "33959",
                plannableDate = "2026-04-01T18:59:00Z",
                htmlUrl = "/courses/33959/assignments/768813",
                newActivity = true,
                contextName = "COMP-362 Sec 001 - Operating Systems",
                contextImage = "https://example.test/img.png",
                plannable = PlannableDto(
                    title = "L08 Submit",
                    dueAt = "2026-04-01T18:59:00Z",
                    pointsPossible = 20.0,
                ),
                submissions = SubmissionsDto(submitted = true, needsGrading = true),
            ),
        )

        repo.sync(windowStart, windowEnd)

        val saved = dao.snapshot().single()
        assertEquals("assignment_768813", saved.id)
        assertEquals("L08 Submit", saved.title)
        assertEquals(20.0, saved.pointsPossible!!, 0.0)
        assertEquals(Instant.parse("2026-04-01T18:59:00Z"), saved.dueAt)
        assertEquals("/courses/33959/assignments/768813", saved.htmlUrl)
        assertTrue(saved.submitted)
        assertTrue(saved.needsGrading)
    }

    @Test
    fun `observeItems maps cached entities to domain models with PlannerKind discriminator`() = runTest {
        dao.seed(
            listOf(
                entity(id = "assignment_1", date = Instant.parse("2026-04-15T00:00:00Z"), plannableType = "assignment"),
                entity(id = "quiz_2", date = Instant.parse("2026-04-20T00:00:00Z"), plannableType = "quiz"),
                entity(id = "discussion_3", date = Instant.parse("2026-04-25T00:00:00Z"), plannableType = "discussion_topic"),
                entity(id = "frobulator_4", date = Instant.parse("2026-04-26T00:00:00Z"), plannableType = "frobulator"),
            ),
        )

        val items = repo.observeItems(windowStart, windowEnd).first()

        assertEquals(4, items.size)
        assertEquals(PlannerKind.ASSIGNMENT, items[0].kind)
        assertEquals(PlannerKind.QUIZ, items[1].kind)
        assertEquals(PlannerKind.DISCUSSION, items[2].kind)
        // Unknown plannable types fall back to UNKNOWN — never crash.
        assertEquals(PlannerKind.UNKNOWN, items[3].kind)
    }

    @Test
    fun `null submissions block in api response yields empty status with all-false flags`() = runTest {
        api.plannerItemsToReturn = listOf(
            PlannerItemDto(
                plannableId = "999",
                plannableType = "calendar_event",
                plannableDate = "2026-04-15T00:00:00Z",
                htmlUrl = "/x",
                plannable = PlannableDto(title = "Office hours"),
                submissions = null,
            ),
        )

        repo.sync(windowStart, windowEnd)

        val saved = dao.snapshot().single()
        assertEquals(false, saved.submitted)
        assertEquals(false, saved.late)
        assertNull(saved.dueAt)
        assertNull(saved.pointsPossible)
    }

    private fun dto(plannableId: String, plannableType: String, date: String) = PlannerItemDto(
        plannableId = plannableId,
        plannableType = plannableType,
        plannableDate = date,
        htmlUrl = "/$plannableType/$plannableId",
        plannable = PlannableDto(title = "title-$plannableId"),
    )

    private fun entity(
        id: String,
        date: Instant,
        plannableType: String = "assignment",
    ) = CanvasPlannerItemEntity(
        id = id,
        plannableType = plannableType,
        plannableId = id.substringAfterLast('_'),
        courseId = null,
        title = "t-$id",
        contextName = null,
        contextImage = null,
        plannableDate = date,
        dueAt = null,
        pointsPossible = null,
        htmlUrl = "/x",
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
