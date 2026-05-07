package com.ekhonavigator.core.canvas.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasCourseDto(
    val id: String,
    val name: String,
    @SerialName("course_code") val courseCode: String,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("image_download_url") val imageDownloadUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val term: CanvasTermDto? = null,
    val enrollments: List<CanvasEnrollmentDto> = emptyList(),
)

@Serializable
data class CanvasTermDto(
    val name: String? = null,
    @SerialName("end_at") val endAt: String? = null,
)

@Serializable
data class CanvasEnrollmentDto(
    val type: String,
    @SerialName("current_score") val currentScore: Double? = null,
    @SerialName("current_grade") val currentGrade: String? = null,
)
