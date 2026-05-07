package com.ekhonavigator.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Per-course announcement cache, populated lazily by
 * `CanvasAnnouncementRepository.sync(courseId)` on detail-screen open.
 *
 * `readAt` is local-only — Canvas doesn't expose announcement-read state
 * over the v1 API at the granularity we need, and round-tripping it would
 * require a separate POST per row. Treat unread-count as a per-device thing
 * for now; future cross-device sync can layer over Firestore if asked for.
 */
@Entity(tableName = "canvas_announcements")
data class CanvasAnnouncementEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val title: String,
    /** HTML body — same render path as `calendar_events.description`. */
    val message: String?,
    val postedAt: Instant?,
    /** Absolutized at sync time via `absolutizeCanvasUrl` — Canvas often returns
     *  this as a relative path that crashes startActivity if launched as-is. */
    val htmlUrl: String?,
    val authorName: String?,
    val authorAvatarUrl: String?,
    /** Local-only "user has expanded this row" timestamp. Null means unread —
     *  drives the unread-dot indicator and the eventual notifications-bell
     *  count. Cleared on signout via `clearAll()`. */
    val readAt: Instant?,
    val lastSyncedAt: Instant,
)
