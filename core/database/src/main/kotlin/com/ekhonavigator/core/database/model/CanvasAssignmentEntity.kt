package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Per-course assignment cache, populated by `CanvasAssignmentRepository.sync()`
 * from the `/courses/:id/assignments?include[]=submission` endpoint. Carries
 * the rich per-assignment context the planner item table can't (description,
 * numeric submission score/grade) — A2.3 surfaces this on the per-class detail
 * screen's Past assignments section AND backfills `calendar_events.description`
 * during sync so EventScreen renders rich text for Canvas-bridged events.
 *
 * Submission fields are flattened (Room doesn't accept nested entity types on
 * nullable values without separate @Embedded scaffolding — three nullable
 * primitives round-trip cleanly).
 */
@Entity(tableName = "canvas_assignments")
data class CanvasAssignmentEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val name: String,
    /** HTML body — sanitized + rendered by EventScreen via the existing
     *  `HtmlDescription` helper that already handles iCal feed bodies. */
    val description: String?,
    val dueAt: Instant?,
    val pointsPossible: Double?,
    /** Absolutized at sync time via `absolutizeCanvasUrl` — Canvas returns
     *  this as a relative path that crashes startActivity if launched as-is. */
    val htmlUrl: String?,
    // Flattened submission fields (no nested entity, no @Embedded for nullable).
    val submissionScore: Double?,
    val submissionGrade: String?,
    val submittedAt: Instant?,
    val gradedAt: Instant?,
    val late: Boolean,
    val missing: Boolean,
    val excused: Boolean,
    /** Canvas's submission `workflow_state` ("submitted", "graded", "unsubmitted",
     *  "pending_review", etc.) — kept raw because the rendering layer interprets
     *  it differently per surface. */
    val workflowState: String?,
    val lastSyncedAt: Instant,
)
