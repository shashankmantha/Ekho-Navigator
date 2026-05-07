package com.ekhonavigator.core.canvas.model

import java.time.Instant

/**
 * Domain projection of a Canvas assignment with its nested submission state.
 * Built from `/courses/:id/assignments?include[]=submission`. The numeric
 * `submission.score` and `submission.grade` are the per-item grade we couldn't
 * surface from the planner endpoint alone — these power the per-class detail
 * screen's "Past assignments + grades" section.
 */
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
    /** Raw Canvas workflow state — "submitted", "graded", "unsubmitted",
     *  "pending_review". Render layer interprets per surface. */
    val workflowState: String?,
) {
    /** Convenience: the assignment is "graded" when Canvas marks the submission
     *  as such — a numeric score may or may not also be present (some
     *  assignments grade "Complete/Incomplete" without a score). */
    val graded: Boolean get() = workflowState == "graded"

    /** True when the user has acted on the assignment in some terminal way —
     *  matches the same predicate used for planner items. Drives the
     *  "Recent submissions" vs "Upcoming" split. */
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
