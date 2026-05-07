package com.ekhonavigator.core.canvas.model

import java.time.Instant

/**
 * Domain projection of a Canvas course announcement. `readAt` is purely
 * device-local — nothing round-trips to Canvas — and drives the unread-dot
 * indicator + the eventual notifications-bell count.
 */
data class CanvasAnnouncement(
    val id: String,
    val courseId: String,
    val title: String,
    val message: String?,
    val postedAt: Instant?,
    val htmlUrl: String?,
    val authorName: String?,
    val authorAvatarUrl: String?,
    val readAt: Instant?,
    val lastSyncedAt: Instant,
) {
    val isUnread: Boolean get() = readAt == null
}
