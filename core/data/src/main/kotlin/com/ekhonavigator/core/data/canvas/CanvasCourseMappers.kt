package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import java.time.Instant

internal fun CanvasCourseDto.toEntity(now: Instant = Instant.now()): CanvasCourseEntity {
    val student = enrollments.firstOrNull { it.type == "student" }
    return CanvasCourseEntity(
        id = id,
        code = courseCode,
        name = name,
        termName = term?.name,
        // Canvas returns ISO-8601; runCatching tolerates malformed/empty strings
        // by leaving termEndAt null, which the current-term filter treats as "ongoing."
        termEndAt = term?.endAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        imageUrl = imageDownloadUrl,
        currentScore = student?.currentScore,
        currentGrade = student?.currentGrade,
        isFavorite = isFavorite,
        lastSyncedAt = now,
    )
}

internal fun CanvasCourseEntity.toDomainModel(): CanvasCourse = CanvasCourse(
    id = id,
    code = code,
    name = name,
    termName = termName,
    termEndAt = termEndAt,
    imageUrl = imageUrl,
    currentScore = currentScore,
    currentGrade = currentGrade,
    isFavorite = isFavorite,
)
