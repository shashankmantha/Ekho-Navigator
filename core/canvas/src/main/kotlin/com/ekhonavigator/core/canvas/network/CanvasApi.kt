package com.ekhonavigator.core.canvas.network

import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentDto
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
        // Canvas's planner endpoint hides items from non-favorited courses unless
        // each is explicitly requested. Pass null to get the default (favorites-only)
        // behavior; pass per-course codes ("course_NNNN") to include all enrolled.
        @Query("context_codes[]") contextCodes: List<String>? = null,
        @Query("per_page") perPage: Int = 100,
    ): Response<List<PlannerItemDto>>

    /**
     * Follows an opaque pagination URL from a `Link: rel="next"` header verbatim.
     * Canvas docs require pagination URLs be treated as opaque — don't try to construct
     * them yourself.
     */
    @GET
    suspend fun getPlannerItemsByUrl(@Url url: String): Response<List<PlannerItemDto>>

    /**
     * Per-course assignment list with each item's submission state nested in.
     * `include[]=submission` is critical — without it the nested submission field
     * is null and we lose access to the per-assignment numeric score/grade that
     * the planner endpoint doesn't carry.
     */
    @GET("api/v1/courses/{courseId}/assignments")
    suspend fun getAssignments(
        @Path("courseId") courseId: String,
        @Query("include[]") include: List<String> = listOf("submission"),
        @Query("per_page") perPage: Int = 100,
    ): Response<List<CanvasAssignmentDto>>

    /** Pagination follow-up for [getAssignments]. Same opaque-URL rule as planner. */
    @GET
    suspend fun getAssignmentsByUrl(@Url url: String): Response<List<CanvasAssignmentDto>>

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
