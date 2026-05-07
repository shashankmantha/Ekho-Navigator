package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasCourse
import kotlinx.coroutines.flow.Flow

interface CanvasCourseRepository {

    fun observeCourses(): Flow<List<CanvasCourse>>

    suspend fun sync(): Result<Unit>

    /** Wipes the local course cache. Call on PAT disconnect / sign-out. */
    suspend fun clearAll()
}
