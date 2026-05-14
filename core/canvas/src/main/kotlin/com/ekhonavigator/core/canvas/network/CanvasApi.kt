package com.ekhonavigator.core.canvas.network

import com.ekhonavigator.core.canvas.network.dto.CanvasAnnouncementDto
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentDto
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentGroupDto
import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

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
        // Null = favorites only (Canvas default). Pass "course_NNNN" codes to
        // include every enrolled course.
        @Query("context_codes[]") contextCodes: List<String>? = null,
        @Query("per_page") perPage: Int = 100,
    ): Response<List<PlannerItemDto>>

    // Canvas pagination URLs are opaque — never construct them yourself.
    @GET
    suspend fun getPlannerItemsByUrl(@Url url: String): Response<List<PlannerItemDto>>

    // include[]=submission is required — otherwise the nested submission is null
    // and we lose access to per-assignment score/grade.
    @GET("api/v1/courses/{courseId}/assignments")
    suspend fun getAssignments(
        @Path("courseId") courseId: String,
        @Query("include[]") include: List<String> = listOf("submission"),
        @Query("per_page") perPage: Int = 100,
    ): Response<List<CanvasAssignmentDto>>

    @GET
    suspend fun getAssignmentsByUrl(@Url url: String): Response<List<CanvasAssignmentDto>>

    // Superset of getAssignments — also carries groupWeight for the weighted average.
    @GET("api/v1/courses/{courseId}/assignment_groups")
    suspend fun getAssignmentGroups(
        @Path("courseId") courseId: String,
        @Query("include[]") include: List<String> = listOf("assignments", "submission"),
        @Query("per_page") perPage: Int = 100,
    ): Response<List<CanvasAssignmentGroupDto>>

    @GET
    suspend fun getAssignmentGroupsByUrl(@Url url: String): Response<List<CanvasAssignmentGroupDto>>

    // context_codes[] takes "course_<id>" — one per course in scope.
    @GET("api/v1/announcements")
    suspend fun getAnnouncements(
        @Query("context_codes[]") contextCodes: List<String>,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("active_only") activeOnly: Boolean = true,
        @Query("per_page") perPage: Int = 50,
    ): Response<List<CanvasAnnouncementDto>>

    @GET
    suspend fun getAnnouncementsByUrl(@Url url: String): Response<List<CanvasAnnouncementDto>>

    companion object {
        // Enough to fill the My Courses card without per-course follow-up calls.
        private val DEFAULT_COURSE_INCLUDES = listOf(
            "term",
            "total_scores",
            "course_image",
            "favorites",
        )
    }
}
