package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasSubmission
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentDto
import com.ekhonavigator.core.canvas.network.dto.CanvasSubmissionDto
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import java.time.Instant

/**
 * @param overrideGroupId when set, takes precedence over the DTO's
 * `assignmentGroupId` field. Used when assignments arrive nested in an
 * assignment_groups payload — the DTO field may be omitted in that response
 * shape, but we already know the group from the parent traversal.
 */
internal fun CanvasAssignmentDto.toEntity(
    now: Instant = Instant.now(),
    overrideGroupId: String? = null,
): CanvasAssignmentEntity {
    val s = submission
    return CanvasAssignmentEntity(
        id = id,
        courseId = courseId,
        name = name,
        description = description,
        // Canvas returns ISO-8601; runCatching tolerates malformed strings by
        // leaving the field null — same pattern as course term parsing.
        dueAt = dueAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pointsPossible = pointsPossible,
        // htmlUrl absolutized in DefaultCanvasAssignmentRepository.sync()
        // against the institution domain.
        htmlUrl = htmlUrl,
        assignmentGroupId = overrideGroupId ?: assignmentGroupId,
        submissionScore = s?.score,
        submissionGrade = s?.grade,
        submittedAt = s?.submittedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        gradedAt = s?.gradedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        late = s?.late ?: false,
        missing = s?.missing ?: false,
        excused = s?.excused ?: false,
        workflowState = s?.workflowState,
        lastSyncedAt = now,
    )
}

internal fun CanvasAssignmentEntity.toDomainModel(): CanvasAssignment = CanvasAssignment(
    id = id,
    courseId = courseId,
    name = name,
    description = description,
    dueAt = dueAt,
    pointsPossible = pointsPossible,
    htmlUrl = htmlUrl,
    submission = CanvasSubmission(
        score = submissionScore,
        grade = submissionGrade,
        submittedAt = submittedAt,
        gradedAt = gradedAt,
        late = late,
        missing = missing,
        excused = excused,
        workflowState = workflowState,
    ),
    lastSyncedAt = lastSyncedAt,
)
