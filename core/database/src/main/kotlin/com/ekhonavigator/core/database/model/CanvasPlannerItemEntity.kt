package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "canvas_planner_items")
data class CanvasPlannerItemEntity(
    /** Composite of "$plannableType_$plannableId" — Canvas IDs aren't globally unique across plannable types. */
    @PrimaryKey val id: String,
    val plannableType: String,
    val plannableId: String,
    val courseId: String?,
    val title: String,
    val contextName: String?,
    val contextImage: String?,
    val plannableDate: Instant,
    val dueAt: Instant?,
    val pointsPossible: Double?,
    val htmlUrl: String,
    val newActivity: Boolean,
    val submitted: Boolean,
    val late: Boolean,
    val missing: Boolean,
    val graded: Boolean,
    val needsGrading: Boolean,
    val hasFeedback: Boolean,
    val excused: Boolean,
    val lastSyncedAt: Instant,
)
