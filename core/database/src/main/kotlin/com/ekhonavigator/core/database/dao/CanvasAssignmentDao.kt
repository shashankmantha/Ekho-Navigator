package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasAssignmentDao {

    /**
     * Per-course assignment list, sorted by due date descending so the
     * Past Assignments section renders most-recent-first by default.
     * Items without a due date sink to the bottom.
     */
    @Query(
        """
        SELECT * FROM canvas_assignments
        WHERE courseId = :courseId
        ORDER BY dueAt DESC
        """
    )
    fun observeForCourse(courseId: String): Flow<List<CanvasAssignmentEntity>>

    @Query("SELECT * FROM canvas_assignments WHERE id = :id")
    suspend fun getById(id: String): CanvasAssignmentEntity?

    @Upsert
    suspend fun upsertAll(items: List<CanvasAssignmentEntity>)

    /**
     * Drops any cached assignment for `courseId` whose id isn't in `keepIds`.
     * Mirrors the planner's range-pruning pattern but scoped per-course since
     * we sync one course at a time (lazy on detail-screen open).
     */
    @Query("DELETE FROM canvas_assignments WHERE courseId = :courseId AND id NOT IN (:keepIds)")
    suspend fun deleteForCourseExcept(courseId: String, keepIds: List<String>)

    @Query("DELETE FROM canvas_assignments")
    suspend fun deleteAll()
}
