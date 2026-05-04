package com.ekhonavigator.core.data.auth

import com.ekhonavigator.core.data.canvas.CanvasAssignmentRepository
import com.ekhonavigator.core.data.canvas.CanvasCourseRepository
import com.ekhonavigator.core.data.canvas.CanvasPlannerRepository
import com.ekhonavigator.core.data.di.ApplicationScope
import com.ekhonavigator.core.data.repository.CalendarRepository
import com.ekhonavigator.core.data.repository.CustomEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for reacting to Firebase auth changes. On sign-in
 * boots per-user listeners + restores user-scoped Firestore state; on sign-out
 * tears down listeners and wipes every per-user local cache (Room + encrypted
 * PAT store). Replaces the old imperative chain that lived in `MainActivity`
 * + `AccountScreen` and required every screen to remember to fan out cleanup.
 *
 * Boot once from `Application.onCreate` — the singleton scope keeps the
 * collection alive for process lifetime.
 */
@Singleton
class AuthLifecycleObserver @Inject constructor(
    private val authRepository: AuthRepository,
    private val customEventRepository: CustomEventRepository,
    private val calendarRepository: CalendarRepository,
    private val canvasCourseRepository: CanvasCourseRepository,
    private val canvasPlannerRepository: CanvasPlannerRepository,
    private val canvasAssignmentRepository: CanvasAssignmentRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            // Track transitions, not raw values. FirebaseAuth's listener fires
            // `null` immediately on app launch BEFORE asynchronously restoring
            // the cached session — treating that initial null as a sign-out
            // would wipe PAT + institution + Room caches just before the real
            // uid arrives, leaving the user "signed in but disconnected" until
            // they re-add their PAT manually.
            //
            // Only fire onSignOut when uid actually transitions from
            // non-null to null. Startup-null is observational, not a sign-out.
            var previousUid: String? = null
            authRepository.userFlow()
                .distinctUntilChanged()
                .collect { uid ->
                    when {
                        uid != null -> onSignIn()
                        previousUid != null -> onSignOut()
                        // else: startup null, do nothing
                    }
                    previousUid = uid
                }
        }
    }

    private suspend fun onSignIn() {
        customEventRepository.startSync(scope)
        // restoreBookmarks() wipes Room bookmarks first, then hydrates from
        // Firestore — so anything bookmarked while signed-out (or by another
        // local user) doesn't survive into this session.
        runCatching { calendarRepository.restoreBookmarks() }
        // Best-effort Canvas resync. Only does anything when a PAT is already
        // present (e.g. app relaunch with the same uid still connected).
        // For a fresh sign-in after sign-out, the PAT was wiped — the user
        // re-connects via Settings, which fires its own immediate sync.
        // NoCanvasAccountException is expected here; runCatching swallows it.
        runCatching { canvasCourseRepository.sync() }
        runCatching { canvasPlannerRepository.sync(plannerWindowStart(), plannerWindowEnd()) }
    }

    private suspend fun onSignOut() {
        // Each step is wrapped independently — one failure cannot block the
        // rest. Without this, a Room/Firestore hiccup in one step silently
        // skips every later step, leaving stale Room state visible in the UI.
        // Bookmark clearing runs first since it's the most user-visible cleanup.
        runCatching { calendarRepository.onSignOut() }
        runCatching { customEventRepository.onSignOut() }
        // Wipe per-user Room data on sign-out so it doesn't leak between
        // accounts on a shared device. PAT + institution are NOT wiped here —
        // they're encrypted and uid-keyed (`pat__${firebaseUid}__$domain` and
        // `domain__$uid`), so a different uid on the same device cannot read
        // them. Keeping them lets the same user sign back in and have Canvas
        // resume automatically. Explicit "Disconnect Canvas" still wipes both.
        runCatching { canvasCourseRepository.clearAll() }
        // clearAll() wipes BOTH the planner cache AND the calendar_events rows
        // bridged from it. Wiping just the planner table leaves Canvas event
        // pills lingering on the calendar.
        runCatching { canvasPlannerRepository.clearAll() }
        // Per-course assignment cache (A2.3) — same strict-isolation rule.
        runCatching { canvasAssignmentRepository.clearAll() }
    }

    private fun plannerWindowStart() =
        YearMonth.now().minusMonths(2).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

    private fun plannerWindowEnd() =
        YearMonth.now().plusMonths(3).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
}
