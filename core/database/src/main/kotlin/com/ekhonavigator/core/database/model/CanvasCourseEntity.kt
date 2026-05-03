package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "canvas_courses")
data class CanvasCourseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val termName: String?,
    /** When this course's term ends. Null means Canvas didn't supply an end_at
     *  (often training/advising courses); treated as "no end" by current-term filters. */
    val termEndAt: Instant?,
    val imageUrl: String?,
    val currentScore: Double?,
    val currentGrade: String?,
    val isFavorite: Boolean,
    val lastSyncedAt: Instant,
)
