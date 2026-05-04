package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape returned by `GET /api/v1/courses/:id/assignments?include[]=submission`.
 * Brings the rich per-assignment context the planner endpoint doesn't carry —
 * `description` (HTML body), `points_possible`, and the nested `submission`
 * payload with the actual numeric `score` / `grade` per item.
 */
@Serializable
data class CanvasAssignmentDto(
    val id: String,
    val name: String,
    @SerialName("course_id") val courseId: String,
    /** HTML body of the assignment. Backfilled into `calendar_events.description`
     *  during sync so EventScreen renders rich text for Canvas events. */
    val description: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("points_possible") val pointsPossible: Double? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    /** Nested submission payload — only present because we passed
     *  `include[]=submission`. Without that include this field would be null. */
    val submission: CanvasSubmissionDto? = null,
)

/**
 * The submission slice nested under each assignment when fetched with
 * `include[]=submission`. `score` is the raw numeric points earned;
 * `grade` is the formatted display ("A-", "92.4%", "Complete", etc.) — Canvas
 * leaves the formatting choice to the instructor's grading scheme.
 */
@Serializable
data class CanvasSubmissionDto(
    val score: Double? = null,
    val grade: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("graded_at") val gradedAt: String? = null,
    val late: Boolean = false,
    val missing: Boolean = false,
    val excused: Boolean = false,
    @SerialName("workflow_state") val workflowState: String? = null,
)
