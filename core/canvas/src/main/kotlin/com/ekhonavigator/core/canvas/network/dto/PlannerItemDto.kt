package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlannerItemDto(
    @SerialName("plannable_id") val plannableId: String,
    @SerialName("plannable_type") val plannableType: String,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("plannable_date") val plannableDate: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("new_activity") val newActivity: Boolean = false,
    @SerialName("context_name") val contextName: String? = null,
    @SerialName("context_image") val contextImage: String? = null,
    val plannable: PlannableDto = PlannableDto(),
    val submissions: SubmissionsDto? = null,
)

@Serializable
data class PlannableDto(
    val title: String? = null,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("points_possible") val pointsPossible: Double? = null,
)

@Serializable
data class SubmissionsDto(
    val submitted: Boolean = false,
    val late: Boolean = false,
    val missing: Boolean = false,
    val graded: Boolean = false,
    @SerialName("needs_grading") val needsGrading: Boolean = false,
    @SerialName("has_feedback") val hasFeedback: Boolean = false,
    val excused: Boolean = false,
)
