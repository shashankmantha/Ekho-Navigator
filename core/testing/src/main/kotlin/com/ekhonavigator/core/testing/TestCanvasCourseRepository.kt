package com.ekhonavigator.core.testing

import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake [CanvasCourseRepository] for unit tests.
 *
 * Tests can seed an initial course list and observe it. `sync()` defaults to
 * `Result.success(Unit)`; override [nextSyncResult] to drive failure paths.
 */
class TestCanvasCourseRepository(
    initial: List<CanvasCourse> = emptyList(),
) : CanvasCourseRepository {

    private val coursesFlow = MutableStateFlow(initial)

    var syncCalls = 0
    var clearAllCalls = 0
    var nextSyncResult: Result<Unit> = Result.success(Unit)

    fun setCourses(courses: List<CanvasCourse>) {
        coursesFlow.value = courses
    }

    override fun observeCourses(): Flow<List<CanvasCourse>> = coursesFlow

    override suspend fun sync(): Result<Unit> {
        syncCalls++
        return nextSyncResult
    }

    override suspend fun clearAll() {
        clearAllCalls++
        coursesFlow.value = emptyList()
    }
}
