package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
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

    /**
     * Fetches the full assignment list for `courseId` from Canvas (paginated)
     * and reconciles the cache. Backfills `calendar_events.description` for
     * matching planner-bridged rows as a side-effect — see implementation.
     */
    suspend fun sync(courseId: String): Result<Unit>

    /** Wipes the entire assignment cache. Call on PAT disconnect / sign-out. */
    suspend fun clearAll()
}
