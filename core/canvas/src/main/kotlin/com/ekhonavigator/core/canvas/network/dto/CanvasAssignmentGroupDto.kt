package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// /assignment_groups is the only endpoint that exposes groupWeight, which
// powers the weighted course average.
@Serializable
data class CanvasAssignmentGroupDto(
    val id: String,
    val name: String,
    // 0-100. Null on ungraded courses or when weighting is off.
    @SerialName("group_weight") val groupWeight: Double? = null,
    val position: Int? = null,
    // Only populated when the call passed include[]=assignments.
    val assignments: List<CanvasAssignmentDto> = emptyList(),
)
