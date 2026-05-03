package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasCourse
import kotlinx.coroutines.flow.Flow

interface CanvasCourseRepository {

    fun observeCourses(): Flow<List<CanvasCourse>>

    suspend fun sync(): Result<Unit>
}
