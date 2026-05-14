package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// GET /api/v1/courses/:id/assignments?include[]=submission
@Serializable
data class CanvasAssignmentDto(
    val id: String,
    val name: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("assignment_group_id") val assignmentGroupId: String? = null,
    val description: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("points_possible") val pointsPossible: Double? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    // Only present when the call passed include[]=submission.
    val submission: CanvasSubmissionDto? = null,
)

// score is the raw points earned; grade is the formatted display ("A-",
// "92.4%", "Complete", etc.) chosen by the instructor's grading scheme.
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
