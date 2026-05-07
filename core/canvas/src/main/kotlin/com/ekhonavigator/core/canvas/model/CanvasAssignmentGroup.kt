package com.ekhonavigator.core.canvas.model

import java.time.Instant

/**
 * Domain projection of a Canvas grading-scheme bucket. Composed at read time
 * from the assignment-groups table joined to the assignments table — the
 * `assignments` slot below is hydrated by the ViewModel rather than the DAO,
 * so the persistence schema stays flat and the domain shape matches what the
 * GradeSummarySection actually needs.
 */
data class CanvasAssignmentGroup(
    val id: String,
    val courseId: String,
    val name: String,
    /** Percent contribution to the final grade (0-100). Null when the
     *  course doesn't use weighted grading. */
    val weight: Double?,
    val position: Int?,
    val assignments: List<CanvasAssignment>,
    val lastSyncedAt: Instant,
)
