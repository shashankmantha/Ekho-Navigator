package com.ekhonavigator.core.data.canvas

import android.util.Log
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentDto
import com.ekhonavigator.core.canvas.network.nextPageUrl
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.CanvasAssignmentDao
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasAssignmentRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val accountSource: CanvasAccountSource,
    private val assignmentDao: CanvasAssignmentDao,
    private val calendarEventDao: CalendarEventDao,
) : CanvasAssignmentRepository {

    override fun observeForCourse(courseId: String): Flow<List<CanvasAssignment>> =
        assignmentDao.observeForCourse(courseId)
            .map { entities -> entities.map(CanvasAssignmentEntity::toDomainModel) }

    override suspend fun sync(courseId: String): Result<Unit> {
        Log.d(TAG, "sync starting course=$courseId")
        return runCatching {
            val api = apiProvider.current()
                ?: throw NoCanvasAccountException.also { Log.d(TAG, "sync skipped: no Canvas account connected") }

            val dtos = fetchAllPages(api, courseId)
            Log.d(TAG, "sync: Canvas returned ${dtos.size} assignments for course $courseId")

            // Absolutize htmlUrl per assignment so Open-in-Canvas launches don't
            // crash on the relative path Canvas sends. Same pattern as planner +
            // course sync.
            val domain = accountSource.currentOrNull()?.domain
            val entities = dtos.map { dto ->
                val entity = dto.toEntity()
                // Capture into a local so smart-cast applies — `htmlUrl` is a
                // public property on a cross-module entity, which the compiler
                // can't smart-cast on its own.
                val rawUrl = entity.htmlUrl
                if (domain != null && !rawUrl.isNullOrBlank()) {
                    entity.copy(htmlUrl = absolutizeCanvasUrl(rawUrl, domain))
                } else entity
            }

            assignmentDao.upsertAll(entities)
            assignmentDao.deleteForCourseExcept(courseId, entities.map { it.id })

            backfillDescriptionsToCalendarEvents(entities)
            Unit
        }.onFailure { e ->
            Log.w(TAG, "sync failed for course $courseId: ${e.message}", e)
        }
    }

    override suspend fun clearAll() {
        assignmentDao.deleteAll()
    }

    /**
     * Walks Canvas's `Link: rel="next"` pagination chain. Per-course assignment
     * lists rarely exceed one page in practice (most courses have <100), but the
     * cap protects against runaway loops if Canvas ever returns a malformed
     * Link header.
     */
    private suspend fun fetchAllPages(api: CanvasApi, courseId: String): List<CanvasAssignmentDto> {
        val all = mutableListOf<CanvasAssignmentDto>()
        var response = api.getAssignments(courseId)
        all += response.body().orEmpty()
        var nextUrl = nextPageUrl(response.headers())
        var pages = 1
        while (nextUrl != null && pages < MAX_PAGES) {
            response = api.getAssignmentsByUrl(nextUrl)
            all += response.body().orEmpty()
            nextUrl = nextPageUrl(response.headers())
            pages++
        }
        if (nextUrl != null) {
            Log.w(TAG, "sync: hit safety cap of $MAX_PAGES pages — newer assignments may be missing")
        }
        Log.d(TAG, "sync: walked $pages page(s) of assignments")
        return all
    }

    /**
     * Backfills assignment description onto matching `calendar_events` rows so
     * EventScreen renders rich text for Canvas events without an extra read-time
     * join. Match key is the planner-bridged uid format: `assignment_${id}`.
     *
     * Logs the join hit-rate as cheap insurance — if Canvas ever changes the id
     * shape we'll see the rate drop in logcat the first time we test.
     */
    private suspend fun backfillDescriptionsToCalendarEvents(entities: List<CanvasAssignmentEntity>) {
        var matched = 0
        for (a in entities) {
            val description = a.description ?: continue
            if (description.isBlank()) continue
            val plannerBridgedUid = "assignment_${a.id}"
            // Skip the update entirely if the row doesn't exist — UPDATE on a
            // non-existent row would be a silent no-op anyway, but the existence
            // check lets us count match rate honestly.
            val existing = calendarEventDao.getEventById(plannerBridgedUid)
            if (existing != null) {
                calendarEventDao.updateDescription(plannerBridgedUid, description)
                matched++
            }
        }
        Log.d(
            TAG,
            "sync: description backfill matched $matched/${entities.size} assignments to calendar_events rows",
        )
    }

    companion object {
        private const val TAG = "CanvasAssignmentSync"
        private const val MAX_PAGES = 10
    }
}
