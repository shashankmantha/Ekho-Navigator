package com.ekhonavigator.core.data.canvas

import android.util.Log
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasAssignmentGroup
import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentGroupDto
import com.ekhonavigator.core.canvas.network.nextPageUrl
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.CanvasAssignmentDao
import com.ekhonavigator.core.database.dao.CanvasAssignmentGroupDao
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import com.ekhonavigator.core.database.model.CanvasAssignmentGroupEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasAssignmentRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val accountSource: CanvasAccountSource,
    private val assignmentDao: CanvasAssignmentDao,
    private val groupDao: CanvasAssignmentGroupDao,
    private val calendarEventDao: CalendarEventDao,
) : CanvasAssignmentRepository {

    override fun observeForCourse(courseId: String): Flow<List<CanvasAssignment>> =
        assignmentDao.observeForCourse(courseId)
            .map { entities -> entities.map(CanvasAssignmentEntity::toDomainModel) }

    override fun observeById(assignmentId: String): Flow<CanvasAssignment?> =
        assignmentDao.observeById(assignmentId)
            .map { entity -> entity?.toDomainModel() }

    override fun observeGroupsForCourse(courseId: String): Flow<List<CanvasAssignmentGroup>> =
        combine(
            groupDao.observeForCourse(courseId),
            assignmentDao.observeForCourse(courseId),
        ) { groupEntities, assignmentEntities ->
            // Read-side join via the foreign-id column. The domain model
            // doesn't surface assignmentGroupId, so build the id→groupId
            // lookup from the entities up-front and groupBy against it. Any
            // assignment with a null groupId drops into the synthetic
            // "Other" bucket composed below.
            val groupIdByAssignment = assignmentEntities.associate { it.id to it.assignmentGroupId }
            val assignmentsByGroup = assignmentEntities
                .map(CanvasAssignmentEntity::toDomainModel)
                .groupBy { groupIdByAssignment[it.id] }
            val knownGroups = groupEntities.map { entity ->
                entity.toDomainModel(assignmentsByGroup[entity.id].orEmpty())
            }
            // Synthetic group catches ungrouped assignments; only included when
            // non-empty so a fully-categorized course doesn't grow an empty bucket.
            val ungrouped = assignmentsByGroup[null].orEmpty()
            if (ungrouped.isEmpty()) knownGroups
            else knownGroups + CanvasAssignmentGroup(
                id = "${courseId}_ungrouped",
                courseId = courseId,
                name = "Other",
                weight = null,
                position = Int.MAX_VALUE,
                assignments = ungrouped,
                lastSyncedAt = ungrouped.maxOf { it.lastSyncedAt },
            )
        }

    override suspend fun sync(courseId: String): Result<Unit> {
        Log.d(TAG, "sync starting course=$courseId")
        return runCatching {
            val api = apiProvider.current()
                ?: throw NoCanvasAccountException.also { Log.d(TAG, "sync skipped: no Canvas account connected") }

            val groupDtos = fetchAllPages(api, courseId)
            val flatAssignments = groupDtos.flatMap { g ->
                g.assignments.map { it to g.id }
            }
            Log.d(
                TAG,
                "sync: Canvas returned ${groupDtos.size} groups / ${flatAssignments.size} assignments for course $courseId",
            )

            val domain = accountSource.currentOrNull()?.domain

            // Group entities first — they hold the weights the UI joins to.
            val groupEntities = groupDtos.map { it.toEntity(courseId = courseId) }
            groupDao.upsertAll(groupEntities)
            groupDao.deleteForCourseExcept(courseId, groupEntities.map { it.id })

            // Assignment entities second; tag each with its parent group so the
            // read-side join on the foreign-id column works regardless of whether
            // Canvas duplicated the field on the assignment payload.
            val assignmentEntities = flatAssignments.map { (dto, groupId) ->
                val entity = dto.toEntity(overrideGroupId = groupId)
                // Local-val capture for cross-module smart-cast on htmlUrl —
                // same workaround as the planner + course sync paths.
                val rawUrl = entity.htmlUrl
                if (domain != null && !rawUrl.isNullOrBlank()) {
                    entity.copy(htmlUrl = absolutizeCanvasUrl(rawUrl, domain))
                } else entity
            }
            assignmentDao.upsertAll(assignmentEntities)
            assignmentDao.deleteForCourseExcept(courseId, assignmentEntities.map { it.id })

            backfillDescriptionsToCalendarEvents(assignmentEntities)
            Unit
        }.onFailure { e ->
            Log.w(TAG, "sync failed for course $courseId: ${e.message}", e)
        }
    }

    override suspend fun clearAll() {
        assignmentDao.deleteAll()
        groupDao.deleteAll()
    }

    /**
     * Walks Canvas's `Link: rel="next"` pagination chain on the
     * assignment_groups endpoint. Per-course group counts are tiny (a handful
     * of buckets) so multi-page responses are unlikely, but the safety cap
     * mirrors the assignments-only path it replaces.
     */
    private suspend fun fetchAllPages(api: CanvasApi, courseId: String): List<CanvasAssignmentGroupDto> {
        val all = mutableListOf<CanvasAssignmentGroupDto>()
        var response = api.getAssignmentGroups(courseId)
        all += response.body().orEmpty()
        var nextUrl = nextPageUrl(response.headers())
        var pages = 1
        while (nextUrl != null && pages < MAX_PAGES) {
            response = api.getAssignmentGroupsByUrl(nextUrl)
            all += response.body().orEmpty()
            nextUrl = nextPageUrl(response.headers())
            pages++
        }
        if (nextUrl != null) {
            Log.w(TAG, "sync: hit safety cap of $MAX_PAGES pages — newer groups may be missing")
        }
        Log.d(TAG, "sync: walked $pages page(s) of assignment_groups")
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
