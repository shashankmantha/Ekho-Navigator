package com.ekhonavigator.core.data.canvas

import android.util.Log
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.CanvasAnnouncement
import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.CanvasAnnouncementDto
import com.ekhonavigator.core.canvas.network.nextPageUrl
import com.ekhonavigator.core.database.dao.CanvasAnnouncementDao
import com.ekhonavigator.core.database.model.CanvasAnnouncementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasAnnouncementRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val accountSource: CanvasAccountSource,
    private val announcementDao: CanvasAnnouncementDao,
) : CanvasAnnouncementRepository {

    override fun observeForCourse(courseId: String): Flow<List<CanvasAnnouncement>> =
        announcementDao.observeForCourse(courseId)
            .map { entities -> entities.map(CanvasAnnouncementEntity::toDomainModel) }

    override fun observeAll(): Flow<List<CanvasAnnouncement>> =
        announcementDao.observeAll()
            .map { entities -> entities.map(CanvasAnnouncementEntity::toDomainModel) }

    override fun observeUnreadCount(): Flow<Int> = announcementDao.observeUnreadCount()

    override suspend fun sync(courseId: String): Result<Unit> {
        Log.d(TAG, "sync starting course=$courseId")
        return runCatching {
            val api = apiProvider.current()
                ?: throw NoCanvasAccountException.also { Log.d(TAG, "sync skipped: no Canvas account connected") }

            val now = Instant.now()
            val window = announcementWindow(now)
            val dtos = fetchAllPages(
                api = api,
                courseId = courseId,
                startDate = window.start,
                endDate = window.end,
            )
            Log.d(TAG, "sync: Canvas returned ${dtos.size} announcements for course $courseId")

            // Preserve per-row readAt — `@Upsert` rewrites every column, so
            // grab the current state once and merge it into the inbound DTOs
            // before we hand them to the upsert.
            val priorReadState = announcementDao.getReadStateForCourse(courseId)
                .associate { it.id to it.readAt }

            val domain = accountSource.currentOrNull()?.domain
            val entities = dtos.map { dto ->
                val entity = dto.toEntity(now = now, preservedReadAt = priorReadState[dto.id])
                // Local-val capture for cross-module smart-cast on htmlUrl —
                // same workaround as the planner / assignment / course paths.
                val rawUrl = entity.htmlUrl
                if (domain != null && !rawUrl.isNullOrBlank()) {
                    entity.copy(htmlUrl = absolutizeCanvasUrl(rawUrl, domain))
                } else entity
            }

            announcementDao.upsertAll(entities)
            announcementDao.deleteForCourseExcept(courseId, entities.map { it.id })
            Unit
        }.onFailure { e ->
            Log.w(TAG, "sync failed for course $courseId: ${e.message}", e)
        }
    }

    override suspend fun markRead(announcementId: String) {
        announcementDao.markRead(announcementId, Instant.now())
    }

    override suspend fun clearAll() {
        announcementDao.deleteAll()
    }

    /**
     * Walks Canvas's `Link: rel="next"` pagination chain. Per-course
     * announcement counts are tiny (5-30 typical), but the safety cap matches
     * the assignments path it mirrors.
     */
    private suspend fun fetchAllPages(
        api: CanvasApi,
        courseId: String,
        startDate: String,
        endDate: String,
    ): List<CanvasAnnouncementDto> {
        val all = mutableListOf<CanvasAnnouncementDto>()
        var response = api.getAnnouncements(
            contextCodes = listOf("$COURSE_CONTEXT_PREFIX$courseId"),
            startDate = startDate,
            endDate = endDate,
        )
        all += response.body().orEmpty()
        var nextUrl = nextPageUrl(response.headers())
        var pages = 1
        while (nextUrl != null && pages < MAX_PAGES) {
            response = api.getAnnouncementsByUrl(nextUrl)
            all += response.body().orEmpty()
            nextUrl = nextPageUrl(response.headers())
            pages++
        }
        if (nextUrl != null) {
            Log.w(TAG, "sync: hit safety cap of $MAX_PAGES pages — newer announcements may be missing")
        }
        Log.d(TAG, "sync: walked $pages page(s) of announcements")
        return all
    }

    private data class AnnouncementWindow(val start: String, val end: String)

    /**
     * Trailing 90-day window for the announcements query — Canvas's
     * `start_date`/`end_date` parameters are required.
     */
    private fun announcementWindow(now: Instant): AnnouncementWindow {
        val start = now.minus(WINDOW_DAYS_BACK, ChronoUnit.DAYS)
        return AnnouncementWindow(
            start = DateTimeFormatter.ISO_INSTANT.format(start),
            end = DateTimeFormatter.ISO_INSTANT.format(now),
        )
    }

    companion object {
        private const val TAG = "CanvasAnnouncementSync"
        private const val MAX_PAGES = 5
        private const val WINDOW_DAYS_BACK = 90L
        private const val COURSE_CONTEXT_PREFIX = "course_"
    }
}
