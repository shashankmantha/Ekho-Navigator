package com.ekhonavigator.core.canvas.model

data class CanvasCourse(
    val id: String,
    val code: String,
    val name: String,
    val termName: String?,
    val imageUrl: String?,
    val currentScore: Double?,
    val currentGrade: String?,
    val isFavorite: Boolean,
)
