package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Per-course grading-scheme bucket Canvas exposes via
 * `assignment_groups?include[]=assignments`. Stores the instructor-defined
 * weights ("Homework 30%, Exams 50%, Final 20%") that power the
 * GradeSummarySection's weighted breakdown — the only place Canvas surfaces
 * those weights at all.
 *
 * Joined to `canvas_assignments` at read time via
 * `canvas_assignments.assignmentGroupId`. Pruned per-course like assignments.
 */
@Entity(tableName = "canvas_assignment_groups")
data class CanvasAssignmentGroupEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val name: String,
    /** Percent contribution to the final grade (0-100). Null when the
     *  course doesn't use weighted grading; the read-side falls back to
     *  equal-weight or points-only in that case. */
    val groupWeight: Double?,
    /** Display order Canvas wants groups in. */
    val position: Int?,
    val lastSyncedAt: Instant,
)
