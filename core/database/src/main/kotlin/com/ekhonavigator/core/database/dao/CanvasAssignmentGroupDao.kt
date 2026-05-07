package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.CanvasAssignmentGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasAssignmentGroupDao {

    /** Per-course groups, sorted by Canvas's display position with nulls last. */
    @Query(
        """
        SELECT * FROM canvas_assignment_groups
        WHERE courseId = :courseId
        ORDER BY position IS NULL, position ASC
        """
    )
    fun observeForCourse(courseId: String): Flow<List<CanvasAssignmentGroupEntity>>

    @Upsert
    suspend fun upsertAll(groups: List<CanvasAssignmentGroupEntity>)

    /** Per-course pruning, mirrors the assignments table. */
    @Query("DELETE FROM canvas_assignment_groups WHERE courseId = :courseId AND id NOT IN (:keepIds)")
    suspend fun deleteForCourseExcept(courseId: String, keepIds: List<String>)

    @Query("DELETE FROM canvas_assignment_groups")
    suspend fun deleteAll()
}
