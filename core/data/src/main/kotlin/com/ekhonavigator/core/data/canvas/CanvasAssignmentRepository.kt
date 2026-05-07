package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasAssignmentGroup
import kotlinx.coroutines.flow.Flow

/**
 * Per-course assignment cache + sync coordinator. Backs the per-class detail
 * screen's Past Assignments section — fetched lazily when the user opens a
 * course rather than synced en masse on app launch (one network call per
 * course is cheaper than one per assignment, but still not trivial).
 */
interface CanvasAssignmentRepository {

    /** Stream of cached assignments for the given course, sorted by due date
     *  descending (DAO-side sort). Empty until the first sync completes. */
    fun observeForCourse(courseId: String): Flow<List<CanvasAssignment>>

    /** Reactive single-assignment lookup. Backs the read-time grade backfill on
     *  EventScreen — given a planner-bridged calendar event, the planner item's
     *  `plannableId` is the assignment id we resolve here. Emits null until the
     *  course has been synced (lazy, on per-class-detail open). */
    fun observeById(assignmentId: String): Flow<CanvasAssignment?>

    /** Composed flow of grading-scheme buckets joined to their assignments.
     *  Backs the GradeSummarySection's weighted breakdown — each group carries
     *  its `weight`, and assignments without a Canvas-side group fall into a
     *  synthetic "Other" bucket so they're never dropped from the UI. */
    fun observeGroupsForCourse(courseId: String): Flow<List<CanvasAssignmentGroup>>

    /**
     * Fetches the full assignment list for `courseId` from Canvas (paginated)
     * and reconciles the cache. Backfills `calendar_events.description` for
     * matching planner-bridged rows as a side-effect — see implementation.
     */
    suspend fun sync(courseId: String): Result<Unit>

    /** Wipes the entire assignment cache. Call on PAT disconnect / sign-out. */
    suspend fun clearAll()
}
