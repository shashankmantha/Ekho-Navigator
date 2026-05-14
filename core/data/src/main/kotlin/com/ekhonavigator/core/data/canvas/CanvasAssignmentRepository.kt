package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasAssignmentGroup
import kotlinx.coroutines.flow.Flow

// Synced lazily when the user opens a per-class detail screen, not en masse
// at app launch.
interface CanvasAssignmentRepository {

    fun observeForCourse(courseId: String): Flow<List<CanvasAssignment>>

    fun observeById(assignmentId: String): Flow<CanvasAssignment?>

    // Assignments with no Canvas group fall into a synthetic "Other" bucket
    // so they're never dropped from the UI.
    fun observeGroupsForCourse(courseId: String): Flow<List<CanvasAssignmentGroup>>

    // Side effect: backfills calendar_events.description for matching
    // planner-bridged rows.
    suspend fun sync(courseId: String): Result<Unit>

    suspend fun clearAll()
}
