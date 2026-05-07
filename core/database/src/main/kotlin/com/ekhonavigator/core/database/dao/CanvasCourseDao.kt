package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasCourseDao {

    @Query("SELECT * FROM canvas_courses ORDER BY isFavorite DESC, name ASC")
    fun observeAll(): Flow<List<CanvasCourseEntity>>

    @Query("SELECT * FROM canvas_courses WHERE id = :id")
    suspend fun getById(id: String): CanvasCourseEntity?

    @Upsert
    suspend fun upsertAll(courses: List<CanvasCourseEntity>)

    @Query("DELETE FROM canvas_courses WHERE id NOT IN (:keepIds)")
    suspend fun deleteOthers(keepIds: List<String>)

    @Query("DELETE FROM canvas_courses")
    suspend fun deleteAll()
}
