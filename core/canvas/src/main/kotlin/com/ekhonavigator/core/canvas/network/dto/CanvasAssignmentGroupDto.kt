package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shape returned by
 *   `GET /api/v1/courses/:id/assignment_groups?include[]=assignments&include[]=submission`.
 *
 * Strict superset of the bare `/assignments` endpoint — the same per-assignment
 * submission payload is nested inside each group's `assignments` array, plus
 * we get the group's `groupWeight` which is the only place Canvas exposes the
 * grading-scheme weights powering the weighted course average.
 */
@Serializable
data class CanvasAssignmentGroupDto(
    val id: String,
    val name: String,
    /** Percent contribution of this group to the final grade (0-100). Null on
     *  ungraded courses or when the instructor hasn't enabled weighted grading;
     *  the consumer falls back to equal-weight or points-only when null. */
    @SerialName("group_weight") val groupWeight: Double? = null,
    /** Sort order Canvas wants groups displayed in. Surface in the same order
     *  the instructor set up. */
    val position: Int? = null,
    /** Assignments in this group. Only populated because we passed
     *  `include[]=assignments` — without it this field is empty. */
    val assignments: List<CanvasAssignmentDto> = emptyList(),
)
