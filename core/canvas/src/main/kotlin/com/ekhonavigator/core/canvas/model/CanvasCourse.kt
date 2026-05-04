package com.ekhonavigator.core.canvas.model

import java.time.Instant

data class CanvasCourse(
    val id: String,
    val code: String,
    val name: String,
    val termName: String?,
    /** Term end timestamp from Canvas, when supplied. Null = treat as ongoing. */
    val termEndAt: Instant?,
    val imageUrl: String?,
    val currentScore: Double?,
    val currentGrade: String?,
    val isFavorite: Boolean,
    /** Absolute Canvas web URL for the course landing page. Used by the
     *  "Open in Canvas" deep link on the per-class detail screen. */
    val htmlUrl: String? = null,
)
