package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import kotlinx.coroutines.flow.Flow

/**
 * Per-course announcement cache + sync coordinator. Backs the per-class detail
 * screen's Announcements section AND the Campus tab's cross-course feed.
 *
 * Sync runs lazily on detail-screen open (mirrors the assignment_groups
 * pattern) — announcement counts are tiny, but per-course fanning at sign-in
 * would still fire one network call per enrolled course.
 */
interface CanvasAnnouncementRepository {

    /** Stream of cached announcements for the given course, newest-first. */
    fun observeForCourse(courseId: String): Flow<List<CanvasAnnouncement>>

    /** Cross-course feed used by the Campus tab home-base section in A3. */
    fun observeAll(): Flow<List<CanvasAnnouncement>>

    /** Reactive unread count for the eventual notifications bell. */
    fun observeUnreadCount(): Flow<Int>

    /**
     * Fetches a 90-day trailing window of announcements for `courseId` and
     * reconciles the cache. Per-row `readAt` is preserved across the upsert
     * so the user's read-state isn't wiped on every sync.
     */
    suspend fun sync(courseId: String): Result<Unit>

    /** Stamps the row's `readAt` to now. Called by the UI on first row expand. */
    suspend fun markRead(announcementId: String)

    /** Wipes the entire cache. Call on PAT disconnect / sign-out. */
    suspend fun clearAll()
}
