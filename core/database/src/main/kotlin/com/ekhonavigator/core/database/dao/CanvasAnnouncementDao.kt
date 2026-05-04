package com.ekhonavigator.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ekhonavigator.core.database.model.CanvasAnnouncementEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CanvasAnnouncementDao {

    /** Per-course announcement feed sorted newest-first. */
    @Query(
        """
        SELECT * FROM canvas_announcements
        WHERE courseId = :courseId
        ORDER BY postedAt IS NULL, postedAt DESC
        """
    )
    fun observeForCourse(courseId: String): Flow<List<CanvasAnnouncementEntity>>

    /** Cross-course feed — backs the Campus tab home-base "Recent
     *  announcements" section in A3. Same newest-first sort. */
    @Query(
        """
        SELECT * FROM canvas_announcements
        ORDER BY postedAt IS NULL, postedAt DESC
        """
    )
    fun observeAll(): Flow<List<CanvasAnnouncementEntity>>

    /** Reactive unread counter for the eventual notifications bell — null
     *  `readAt` means the user hasn't expanded the row yet. */
    @Query("SELECT COUNT(*) FROM canvas_announcements WHERE readAt IS NULL")
    fun observeUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(items: List<CanvasAnnouncementEntity>)

    /** Per-course pruning, mirrors the assignments + groups tables. */
    @Query("DELETE FROM canvas_announcements WHERE courseId = :courseId AND id NOT IN (:keepIds)")
    suspend fun deleteForCourseExcept(courseId: String, keepIds: List<String>)

    /** Read-state lookup used by the repo to preserve `readAt` across an
     *  upsert sync — `@Upsert` rewrites every column, so the repo reads the
     *  current state, merges it into the inbound entities, then upserts. */
    @Query("SELECT id, readAt FROM canvas_announcements WHERE courseId = :courseId")
    suspend fun getReadStateForCourse(courseId: String): List<AnnouncementReadState>

    /** Targeted read-state update — preserves every other field on the row,
     *  including the synced HTML body. */
    @Query("UPDATE canvas_announcements SET readAt = :readAt WHERE id = :id")
    suspend fun markRead(id: String, readAt: Instant)

    @Query("DELETE FROM canvas_announcements")
    suspend fun deleteAll()
}

/** Lightweight projection for [CanvasAnnouncementDao.getReadStateForCourse]. */
data class AnnouncementReadState(
    val id: String,
    val readAt: Instant?,
)
