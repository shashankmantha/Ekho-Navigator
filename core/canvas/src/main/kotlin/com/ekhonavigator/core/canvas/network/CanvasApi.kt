package com.ekhonavigator.core.canvas.network

import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CanvasApi {

    @GET("api/v1/courses")
    suspend fun getCourses(
        @Query("enrollment_state") enrollmentState: String = "active",
        @Query("per_page") perPage: Int = 100,
        @Query("include[]") include: List<String> = DEFAULT_COURSE_INCLUDES,
    ): List<CanvasCourseDto>

    @GET("api/v1/planner/items")
    suspend fun getPlannerItems(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("per_page") perPage: Int = 100,
    ): List<PlannerItemDto>

    companion object {
        // Picked to fill the My Courses card without follow-up calls per course.
        private val DEFAULT_COURSE_INCLUDES = listOf(
            "term",
            "total_scores",
            "course_image",
            "favorites",
        )
    }
}
