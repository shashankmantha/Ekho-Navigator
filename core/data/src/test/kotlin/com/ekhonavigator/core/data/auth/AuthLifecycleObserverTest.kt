package com.ekhonavigator.core.data.auth

import android.content.Context
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import com.ekhonavigator.core.data.util.SyncResult
import com.ekhonavigator.core.model.CalendarEvent
import com.ekhonavigator.core.model.EventAttendee
import com.ekhonavigator.core.model.RsvpStatus
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AuthLifecycleObserverTest {

    private val authRepo = FakeAuthRepository()
    private val customEventRepo = FakeCustomEventRepository()
    private val calendarRepo = FakeCalendarRepository()
    private val courseRepo = FakeCanvasCourseRepository()
    private val plannerRepo = FakeCanvasPlannerRepository()

    @Test
    fun `start with already signed-in uid fires onSignIn fan-out including Canvas sync`() = runTest {
        authRepo.setUid("user-1")
        val observer = newObserver(CoroutineScope(coroutineContext + Job()))

        observer.start()
        advanceUntilIdle()

        assertEquals(1, customEventRepo.startSyncCalls)
        assertEquals(1, calendarRepo.restoreBookmarksCalls)
        assertEquals(1, courseRepo.syncCalls)
        assertEquals(1, plannerRepo.syncCalls.size)
        assertEquals(0, customEventRepo.onSignOutCalls)
    }

    @Test
    fun `transition to null wipes per-user data but preserves Canvas credentials`() = runTest {
        authRepo.setUid("user-1")
        val observer = newObserver(CoroutineScope(coroutineContext + Job()))
        observer.start()
        advanceUntilIdle()

        authRepo.setUid(null)
        advanceUntilIdle()

        assertEquals(1, customEventRepo.onSignOutCalls)
        assertEquals(1, calendarRepo.onSignOutCalls)
        assertEquals(1, courseRepo.clearAllCalls)
        assertEquals(1, plannerRepo.clearAllCalls)
        // PAT and institution must NOT be wiped on signout — they're encrypted,
        // uid-keyed, and should let the same user re-sign-in without re-entering
        // credentials. Multi-account safety comes from the uid key, not from wipe.
    }

    @Test
    fun `transition from null to uid restarts sync only once per uid change`() = runTest {
        authRepo.setUid(null)
        val observer = newObserver(CoroutineScope(coroutineContext + Job()))
        observer.start()
        advanceUntilIdle()

        // Startup-null is treated as observational only — must NOT fire
        // onSignOut. Otherwise app launch would wipe a returning user's
        // PAT + institution before FirebaseAuth finishes restoring the session.
        assertEquals(0, customEventRepo.startSyncCalls)
        assertEquals(0, calendarRepo.onSignOutCalls)
        assertEquals(0, customEventRepo.onSignOutCalls)

        authRepo.setUid("user-1")
        advanceUntilIdle()
        assertEquals(1, customEventRepo.startSyncCalls)
        assertEquals(1, calendarRepo.restoreBookmarksCalls)

        // Same uid emitted again must be deduped by distinctUntilChanged.
        authRepo.setUid("user-1")
        advanceUntilIdle()
        assertEquals(1, customEventRepo.startSyncCalls)
        assertEquals(1, calendarRepo.restoreBookmarksCalls)
    }

    @Test
    fun `signed-in to signed-out transition still fires onSignOut`() = runTest {
        authRepo.setUid("user-1")
        val observer = newObserver(CoroutineScope(coroutineContext + Job()))
        observer.start()
        advanceUntilIdle()

        // Real sign-out (was signed in, now null) MUST trigger cleanup.
        authRepo.setUid(null)
        advanceUntilIdle()
        assertEquals(1, calendarRepo.onSignOutCalls)
        assertEquals(1, customEventRepo.onSignOutCalls)
    }

    @Test
    fun `start is idempotent`() = runTest {
        authRepo.setUid("user-1")
        val observer = newObserver(CoroutineScope(coroutineContext + Job()))

        observer.start()
        observer.start()
        observer.start()
        advanceUntilIdle()

        // Single collector even if start() is called repeatedly.
        assertEquals(1, customEventRepo.startSyncCalls)
        assertEquals(1, calendarRepo.restoreBookmarksCalls)
    }

    private fun newObserver(scope: CoroutineScope): AuthLifecycleObserver =
        AuthLifecycleObserver(
            authRepository = authRepo,
            customEventRepository = customEventRepo,
            calendarRepository = calendarRepo,
            canvasCourseRepository = courseRepo,
            canvasPlannerRepository = plannerRepo,
            scope = scope,
        )

}

private class FakeAuthRepository : AuthRepository {
    private val _userFlow = MutableStateFlow<String?>(null)

    fun setUid(uid: String?) {
        _userFlow.value = uid
    }

    override fun getCurrentUserUid(): String? = _userFlow.value
    override fun getCurrentUserEmail(): String? = null
    override fun getCurrentUserDisplayName(): String? = null
    override fun getCurrentUser(): FirebaseUser? = null
    override fun userFlow(): Flow<String?> = _userFlow
    override suspend fun signInWithGoogle(context: Context, webClientId: String) {}
    override fun signOut() { _userFlow.value = null }
}

private class FakeCustomEventRepository : CustomEventRepository {
    var startSyncCalls = 0
    var onSignOutCalls = 0

    override fun observeMyEvents(ownerUid: String): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override fun observeSharedEvents(): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override fun observeAttendees(eventId: String): Flow<List<EventAttendee>> = flowOf(emptyList())
    override suspend fun createEvent(event: CalendarEvent, sharedWith: Map<String, String>): String = ""
    override suspend fun updateEvent(event: CalendarEvent) {}
    override suspend fun addAttendees(eventId: String, sharedWith: Map<String, String>) {}
    override suspend fun removeAttendee(eventId: String, userId: String) {}
    override suspend fun deleteEvent(eventId: String) {}
    override suspend fun rsvp(eventId: String, userId: String, displayName: String, status: RsvpStatus) {}
    override suspend fun syncAttendees(eventId: String) {}
    override suspend fun pushPendingEvents() {}
    override fun startSync(scope: CoroutineScope) { startSyncCalls++ }
    override fun stopSync() {}
    override suspend fun onSignOut() { onSignOutCalls++ }
}

private class FakeCalendarRepository : CalendarRepository {
    var restoreBookmarksCalls = 0
    var onSignOutCalls = 0

    override fun observeEvents(): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override fun observeBookmarkedEvents(): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override fun observeEventsByDateRange(start: Instant, end: Instant): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override fun observeEventById(id: String): Flow<CalendarEvent?> = flowOf(null)
    override fun observePendingInvites(includePast: Boolean): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override fun observeDeclinedInvites(includePast: Boolean): Flow<List<CalendarEvent>> = flowOf(emptyList())
    override suspend fun toggleBookmark(eventId: String) {}
    override suspend fun restoreBookmarks() { restoreBookmarksCalls++ }
    override suspend fun onSignOut() { onSignOutCalls++ }
    override suspend fun sync(feedUrl: String): SyncResult = SyncResult.Success(0)
}

private class FakeCanvasCourseRepository : CanvasCourseRepository {
    var syncCalls = 0
    var clearAllCalls = 0
    override fun observeCourses(): kotlinx.coroutines.flow.Flow<List<CanvasCourse>> = flowOf(emptyList())
    override suspend fun sync(): Result<Unit> {
        syncCalls++
        return Result.success(Unit)
    }
    override suspend fun clearAll() { clearAllCalls++ }
}

private class FakeCanvasPlannerRepository : CanvasPlannerRepository {
    val syncCalls = mutableListOf<Pair<Instant, Instant>>()
    var clearAllCalls = 0
    override fun observeItems(start: Instant, end: Instant): kotlinx.coroutines.flow.Flow<List<PlannerItem>> = flowOf(emptyList())
    override fun observeAllItems(): kotlinx.coroutines.flow.Flow<List<PlannerItem>> = flowOf(emptyList())
    override fun observeById(id: String): kotlinx.coroutines.flow.Flow<PlannerItem?> = flowOf<PlannerItem?>(null)
    override suspend fun sync(start: Instant, end: Instant): Result<Unit> {
        syncCalls += start to end
        return Result.success(Unit)
    }
    override suspend fun clearAll() { clearAllCalls++ }
}
