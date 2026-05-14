package com.ekhonavigator.core.canvas.model

import java.time.Instant

data class CanvasAssignment(
    val id: String,
    val courseId: String,
    val name: String,
    val description: String?,
    val dueAt: Instant?,
    val pointsPossible: Double?,
    val htmlUrl: String?,
    val submission: CanvasSubmission,
    val lastSyncedAt: Instant,
)

data class CanvasSubmission(
    val score: Double?,
    val grade: String?,
    val submittedAt: Instant?,
    val gradedAt: Instant?,
    val late: Boolean,
    val missing: Boolean,
    val excused: Boolean,
    // Raw Canvas state — "submitted", "graded", "unsubmitted", "pending_review".
    val workflowState: String?,
) {
    // Score may be null for "Complete/Incomplete"-style graded assignments.
    val graded: Boolean get() = workflowState == "graded"

    // Drives the "Recent submissions" vs "Upcoming" split.
    val engaged: Boolean get() = submitted || graded || excused

    val submitted: Boolean get() =
        workflowState == "submitted" || workflowState == "graded" || submittedAt != null

    companion object {
        val EMPTY = CanvasSubmission(
            score = null,
            grade = null,
            submittedAt = null,
            gradedAt = null,
            late = false,
            missing = false,
            excused = false,
            workflowState = null,
        )
    }
}
