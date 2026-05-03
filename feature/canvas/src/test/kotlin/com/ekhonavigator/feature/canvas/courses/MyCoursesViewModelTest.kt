package com.ekhonavigator.feature.canvas.courses

import app.cash.turbine.test
import com.ekhonavigator.core.canvas.auth.CanvasAccount
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyCoursesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accountSource = MutableAccountSource()
    private val repository = MutableCanvasCourseRepository()

    @Test
    fun `disconnected account yields Disconnected state`() = runTest {
        accountSource.account = null

        viewModel().uiState.test {
            assertEquals(MyCoursesUiState.Disconnected, awaitItem())
        }
    }

    @Test
    fun `connected account exposes Loaded with cached courses and triggers a sync`() = runTest {
        accountSource.account = CanvasAccount("uid-1", "csuci.instructure.com")
        repository.coursesFlow.value = listOf(course("1", "COMP-362"))

        val vm = viewModel()
        vm.uiState.test {
            val loaded = awaitItem() as MyCoursesUiState.Loaded
            assertEquals(listOf("1"), loaded.courses.map(CanvasCourse::id))
        }
        assertEquals(1, repository.syncCalls)
    }

    @Test
    fun `sync failure surfaces an error string while preserving cached courses`() = runTest {
        accountSource.account = CanvasAccount("uid-1", "csuci.instructure.com")
        repository.coursesFlow.value = listOf(course("1", "COMP-362"))
        repository.nextSyncResult = Result.failure(IllegalStateException("HTTP 500"))

        val vm = viewModel()
        vm.uiState.test {
            // Drop emissions until we observe both the cached courses AND the surfaced error.
            var lastLoaded: MyCoursesUiState.Loaded? = null
            while (lastLoaded?.error == null) {
                lastLoaded = awaitItem() as MyCoursesUiState.Loaded
            }
            assertEquals("HTTP 500", lastLoaded.error)
            assertEquals(listOf("1"), lastLoaded.courses.map(CanvasCourse::id))
        }
    }

    private fun viewModel() = MyCoursesViewModel(repository, accountSource)

    private fun course(id: String, code: String) = CanvasCourse(
        id = id,
        code = code,
        name = "name-$id",
        termName = null,
        termEndAt = null,
        imageUrl = null,
        currentScore = null,
        currentGrade = null,
        isFavorite = false,
    )
}

private class MutableAccountSource(var account: CanvasAccount? = null) : CanvasAccountSource {
    override fun currentOrNull(): CanvasAccount? = account
}

private class MutableCanvasCourseRepository : CanvasCourseRepository {
    val coursesFlow = MutableStateFlow<List<CanvasCourse>>(emptyList())
    var syncCalls = 0
    var nextSyncResult: Result<Unit> = Result.success(Unit)

    override fun observeCourses(): Flow<List<CanvasCourse>> = coursesFlow

    override suspend fun sync(): Result<Unit> {
        syncCalls++
        return nextSyncResult
    }

    override suspend fun clearAll() {
        coursesFlow.value = emptyList()
    }
}
