package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.model.CanvasAssignmentGroup
import com.ekhonavigator.core.canvas.network.dto.CanvasAssignmentGroupDto
import com.ekhonavigator.core.database.model.CanvasAssignmentGroupEntity
import java.time.Instant

internal fun CanvasAssignmentGroupDto.toEntity(
    courseId: String,
    now: Instant = Instant.now(),
): CanvasAssignmentGroupEntity = CanvasAssignmentGroupEntity(
    id = id,
    courseId = courseId,
    name = name,
    groupWeight = groupWeight,
    position = position,
    lastSyncedAt = now,
)

/**
 * Composes a domain group from its persisted shell + the assignments that
 * already point at it. Caller is responsible for the join — the DAO layer
 * stays flat. `assignments` is filtered + sorted by the caller for a reason:
 * GradeSummarySection wants different orderings than PastAssignmentsSection.
 */
internal fun CanvasAssignmentGroupEntity.toDomainModel(
    assignments: List<CanvasAssignment>,
): CanvasAssignmentGroup = CanvasAssignmentGroup(
    id = id,
    courseId = courseId,
    name = name,
    weight = groupWeight,
    position = position,
    assignments = assignments,
    lastSyncedAt = lastSyncedAt,
)
