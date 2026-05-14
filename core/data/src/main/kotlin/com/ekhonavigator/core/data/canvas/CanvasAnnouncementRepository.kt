package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import kotlinx.coroutines.flow.Flow

// Lazy per-course sync on detail-screen open, same pattern as assignment_groups.
interface CanvasAnnouncementRepository {

    fun observeForCourse(courseId: String): Flow<List<CanvasAnnouncement>>

    fun observeAll(): Flow<List<CanvasAnnouncement>>

    fun observeUnreadCount(): Flow<Int>

    // 90-day trailing window. Preserves per-row readAt across the upsert so the
    // user's read-state isn't wiped on every sync.
    suspend fun sync(courseId: String): Result<Unit>

    suspend fun markRead(announcementId: String)

    suspend fun clearAll()
}
