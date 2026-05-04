package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.canvas.network.dto.CanvasEnrollmentDto
import com.ekhonavigator.core.canvas.network.dto.CanvasTermDto
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DefaultCanvasCourseRepositoryTest {

    private val api = FakeCanvasApi()
    private val provider = FakeCanvasApiProvider(api = api)
    private val accountSource = StubAccountSource(
        com.ekhonavigator.core.canvas.auth.CanvasAccount("uid-1", "csuci.instructure.com"),
    )
    private val dao = FakeCanvasCourseDao()
    private val repo = DefaultCanvasCourseRepository(provider, accountSource, dao)

    private class StubAccountSource(
        var account: com.ekhonavigator.core.canvas.auth.CanvasAccount?,
    ) : com.ekhonavigator.core.canvas.auth.CanvasAccountSource {
        override fun currentOrNull(): com.ekhonavigator.core.canvas.auth.CanvasAccount? = account
    }

    @Test
    fun `sync without a Canvas account fails with NoCanvasAccountException and leaves dao untouched`() = runTest {
        provider.api = null
        dao.seed(listOf(entity("kept")))

        val result = repo.sync()

        assertSame(NoCanvasAccountException, result.exceptionOrNull())
        assertEquals(listOf("kept"), dao.snapshot().map { it.id })
        assertEquals(0, api.calls)
    }

    @Test
    fun `sync upserts the courses returned by Canvas and prunes anything not in the response`() = runTest {
        dao.seed(listOf(entity("stale-1"), entity("kept")))
        api.coursesToReturn = listOf(dto("kept"), dto("new"))

        val result = repo.sync()

        assertTrue(result.isSuccess)
        assertEquals(setOf("kept", "new"), dao.snapshot().map { it.id }.toSet())
    }

    @Test
    fun `observeCourses maps cached entities to domain models`() = runTest {
        dao.seed(listOf(entity(id = "1", code = "COMP-362", grade = "B+")))

        val courses = repo.observeCourses().first()

        val course = courses.single()
        assertEquals("1", course.id)
        assertEquals("COMP-362", course.code)
        assertEquals("B+", course.currentGrade)
    }

    @Test
    fun `sync extracts current grade from the student enrollment, ignoring teacher rows`() = runTest {
        api.coursesToReturn = listOf(
            CanvasCourseDto(
                id = "33983",
                name = "ASL-101",
                courseCode = "ASL-101",
                term = CanvasTermDto(name = "Fall 2025"),
                enrollments = listOf(
                    CanvasEnrollmentDto(type = "teacher", currentScore = 100.0, currentGrade = "A"),
                    CanvasEnrollmentDto(type = "student", currentScore = 89.5, currentGrade = "B+"),
                ),
            ),
        )

        repo.sync()

        val saved = dao.snapshot().single()
        assertEquals("B+", saved.currentGrade)
        assertEquals(89.5, saved.currentScore!!, 0.0)
        assertEquals("Fall 2025", saved.termName)
    }

    private fun dto(id: String) = CanvasCourseDto(id = id, name = "n-$id", courseCode = "c-$id")

    private fun entity(
        id: String,
        code: String = "code-$id",
        grade: String? = null,
    ) = CanvasCourseEntity(
        id = id,
        code = code,
        name = "name-$id",
        termName = null,
        termEndAt = null,
        imageUrl = null,
        currentScore = null,
        currentGrade = grade,
        isFavorite = false,
        lastSyncedAt = Instant.EPOCH,
    )
}
