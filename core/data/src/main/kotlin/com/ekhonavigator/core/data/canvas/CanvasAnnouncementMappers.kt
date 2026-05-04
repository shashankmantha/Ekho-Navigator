package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import com.ekhonavigator.core.canvas.network.dto.CanvasAnnouncementDto
import com.ekhonavigator.core.database.model.CanvasAnnouncementEntity
import java.time.Instant

internal fun CanvasAnnouncementDto.toEntity(
    now: Instant = Instant.now(),
    preservedReadAt: Instant? = null,
): CanvasAnnouncementEntity = CanvasAnnouncementEntity(
    id = id,
    // contextCode arrives as `course_<id>` — strip the prefix so the entity
    // stays consistent with every other table's bare-id courseId column.
    courseId = contextCode.removePrefix(COURSE_CONTEXT_PREFIX),
    title = title,
    message = message,
    postedAt = postedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    htmlUrl = htmlUrl,
    authorName = author?.displayName,
    authorAvatarUrl = author?.avatarUrl,
    // Caller threads the previous readAt in via `preservedReadAt` so the upsert
    // doesn't wipe per-row read-state on every sync. Null on first ingest.
    readAt = preservedReadAt,
    lastSyncedAt = now,
)

internal fun CanvasAnnouncementEntity.toDomainModel(): CanvasAnnouncement = CanvasAnnouncement(
    id = id,
    courseId = courseId,
    title = title,
    message = message,
    postedAt = postedAt,
    htmlUrl = htmlUrl,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    readAt = readAt,
    lastSyncedAt = lastSyncedAt,
)

private const val COURSE_CONTEXT_PREFIX = "course_"
