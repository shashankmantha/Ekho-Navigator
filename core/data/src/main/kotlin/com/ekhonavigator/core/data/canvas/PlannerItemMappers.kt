package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.canvas.model.PlannerKind
import com.ekhonavigator.core.canvas.model.PlannerSubmissionStatus
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import com.ekhonavigator.core.canvas.network.dto.SubmissionsDto
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import java.time.Instant

internal fun PlannerItemDto.toEntity(now: Instant = Instant.now()): CanvasPlannerItemEntity {
    val s = submissions ?: SubmissionsDto()
    return CanvasPlannerItemEntity(
        id = compositeId(plannableType, plannableId),
        plannableType = plannableType,
        plannableId = plannableId,
        courseId = courseId,
        title = plannable.title.orEmpty(),
        contextName = contextName,
        contextImage = contextImage,
        plannableDate = Instant.parse(plannableDate),
        dueAt = plannable.dueAt?.let(Instant::parse),
        pointsPossible = plannable.pointsPossible,
        htmlUrl = htmlUrl,
        newActivity = newActivity,
        submitted = s.submitted,
        late = s.late,
        missing = s.missing,
        graded = s.graded,
        needsGrading = s.needsGrading,
        hasFeedback = s.hasFeedback,
        excused = s.excused,
        lastSyncedAt = now,
    )
}

internal fun CanvasPlannerItemEntity.toDomainModel(): PlannerItem = PlannerItem(
    id = id,
    kind = PlannerKind.fromCanvasType(plannableType),
    courseId = courseId,
    title = title,
    contextName = contextName,
    contextImage = contextImage,
    plannableDate = plannableDate,
    dueAt = dueAt,
    pointsPossible = pointsPossible,
    htmlUrl = htmlUrl,
    newActivity = newActivity,
    submission = PlannerSubmissionStatus(
        submitted = submitted,
        late = late,
        missing = missing,
        graded = graded,
        needsGrading = needsGrading,
        hasFeedback = hasFeedback,
        excused = excused,
    ),
)

internal fun compositeId(plannableType: String, plannableId: String): String =
    "${plannableType}_$plannableId"
