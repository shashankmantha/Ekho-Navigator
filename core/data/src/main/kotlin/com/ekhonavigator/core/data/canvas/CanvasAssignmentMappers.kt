package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasSubmission
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentDto
import com.ekhonavigator.core.canvas.network.dto.CanvasSubmissionDto
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import java.time.Instant

// overrideGroupId wins over the DTO's field — assignments nested under an
// assignment_groups response sometimes omit it on the assignment payload.
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
        // runCatching tolerates malformed timestamps by leaving the field null.
        dueAt = dueAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pointsPossible = pointsPossible,
        // Absolutized later in DefaultCanvasAssignmentRepository.sync().
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
