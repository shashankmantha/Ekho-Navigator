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
    val imageUrl: String?,
    val currentScore: Double?,
    val currentGrade: String?,
    val isFavorite: Boolean,
    val lastSyncedAt: Instant,
)
