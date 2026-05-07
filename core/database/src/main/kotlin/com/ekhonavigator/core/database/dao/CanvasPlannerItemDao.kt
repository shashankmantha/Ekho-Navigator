package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CanvasPlannerItemDao {

    @Query(
        """
        SELECT * FROM canvas_planner_items
        WHERE plannableDate >= :start AND plannableDate < :end
        ORDER BY plannableDate ASC
        """
    )
    fun observeInRange(start: Instant, end: Instant): Flow<List<CanvasPlannerItemEntity>>

    /**
     * All cached planner items. Used by the app-root assignment decorator to build
     * a global eventId→courseId / eventId→completion lookup without re-querying per
     * date range. Pruned by the sync window already, so volume stays bounded.
     */
    @Query("SELECT * FROM canvas_planner_items")
    fun observeAll(): Flow<List<CanvasPlannerItemEntity>>

    @Query("SELECT * FROM canvas_planner_items WHERE id = :id")
    suspend fun getById(id: String): CanvasPlannerItemEntity?

    @Upsert
    suspend fun upsertAll(items: List<CanvasPlannerItemEntity>)

    @Query(
        """
        DELETE FROM canvas_planner_items
        WHERE plannableDate >= :start AND plannableDate < :end
          AND id NOT IN (:keepIds)
        """
    )
    suspend fun deleteInRangeExcept(start: Instant, end: Instant, keepIds: List<String>)

    @Query("DELETE FROM canvas_planner_items")
    suspend fun deleteAll()
}
