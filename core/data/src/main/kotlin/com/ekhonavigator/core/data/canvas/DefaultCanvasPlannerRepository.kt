package com.ekhonavigator.core.data.canvas

import android.util.Log
import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import com.ekhonavigator.core.canvas.network.nextPageUrl
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.CanvasPlannerItemDao
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasPlannerRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val accountSource: CanvasAccountSource,
    private val plannerDao: CanvasPlannerItemDao,
    private val calendarEventDao: CalendarEventDao,
    private val courseRepository: CanvasCourseRepository,
) : CanvasPlannerRepository {

    override fun observeItems(start: Instant, end: Instant): Flow<List<PlannerItem>> =
        plannerDao.observeInRange(start, end)
            .map { entities -> entities.map(CanvasPlannerItemEntity::toDomainModel) }

    override fun observeAllItems(): Flow<List<PlannerItem>> =
        plannerDao.observeAll()
            .map { entities -> entities.map(CanvasPlannerItemEntity::toDomainModel) }

    override fun observeById(id: String): Flow<PlannerItem?> =
        plannerDao.observeAll()
            .map { entities -> entities.firstOrNull { it.id == id }?.toDomainModel() }

    override suspend fun sync(start: Instant, end: Instant): Result<Unit> {
        Log.d(TAG, "sync starting window=[$start, $end]")
        return runCatching {
            val api = apiProvider.current()
                ?: throw NoCanvasAccountException.also { Log.d(TAG, "sync skipped: no Canvas account connected") }

            // Canvas's /planner/items returns only favorited-course items unless we
            // explicitly request each course via context_codes[]. Make sure the course
            // cache is populated so we have IDs to pass.
            var courses = courseRepository.observeCourses().first()
            if (courses.isEmpty()) {
                courseRepository.sync().getOrNull()
                courses = courseRepository.observeCourses().first()
            }
            val contextCodes = courses.map { "course_${it.id}" }
            Log.d(TAG, "sync: passing ${contextCodes.size} course context codes")

            val dtos = fetchAllPages(api, contextCodes, start, end)
            Log.d(TAG, "sync: Canvas returned ${dtos.size} planner items")
            // Canvas returns `html_url` as a relative path (`/courses/.../assignments/...`).
            // Store the absolute form so any "Open in Canvas" launch can resolve it
            // without having to plumb the institution domain into every render site.
            // Falls back to the relative URL if no account (shouldn't happen here —
            // the api call above would have already 401'd — but defensive).
            val domain = accountSource.currentOrNull()?.domain
            val entities = dtos.map { dto ->
                dto.toEntity().let { entity ->
                    if (domain != null) entity.copy(htmlUrl = absolutizeCanvasUrl(entity.htmlUrl, domain))
                    else entity
                }
            }
            plannerDao.upsertAll(entities)
            plannerDao.deleteInRangeExcept(start, end, entities.map { it.id })

            val calendarEntities = entities.mapNotNull { it.toCalendarEventOrNull() }
            Log.d(TAG, "sync: bridged ${calendarEntities.size}/${entities.size} into calendar_events")
            calendarEventDao.upsertEvents(calendarEntities)
            calendarEventDao.deleteByExternalSourceInRangeExcept(
                sourceType = CANVAS_PLANNER_ITEM_SOURCE,
                rangeStart = start,
                rangeEnd = end,
                keepUids = calendarEntities.map { it.uid },
            )
            Log.d(TAG, "sync: complete")
            Unit
        }.onFailure { e ->
            Log.w(TAG, "sync failed: ${e.message}", e)
        }
    }

    /**
     * Walks Canvas's `Link: rel="next"` pagination chain until exhausted or a safety cap.
     * Canvas's planner endpoint caps at 100 items per page, so a single call to a
     * 5-month, 10-course window easily spills past page 1. Cap protects against runaway
     * loops if Canvas ever returns a malformed Link header.
     */
    private suspend fun fetchAllPages(
        api: CanvasApi,
        contextCodes: List<String>,
        start: Instant,
        end: Instant,
    ): List<PlannerItemDto> {
        val all = mutableListOf<PlannerItemDto>()
        var response = api.getPlannerItems(
            startDate = ISO.format(start),
            endDate = ISO.format(end),
            contextCodes = contextCodes.takeIf { it.isNotEmpty() },
        )
        all += response.body().orEmpty()
        var nextUrl = nextPageUrl(response.headers())
        var pages = 1
        while (nextUrl != null && pages < MAX_PAGES) {
            response = api.getPlannerItemsByUrl(nextUrl)
            all += response.body().orEmpty()
            nextUrl = nextPageUrl(response.headers())
            pages++
        }
        if (nextUrl != null) {
            Log.w(TAG, "sync: hit safety cap of $MAX_PAGES pages — newer items may be missing")
        }
        Log.d(TAG, "sync: walked $pages page(s) of planner items")
        return all
    }

    override suspend fun clearAll() {
        plannerDao.deleteAll()
        // The bridged rows in calendar_events are what surface as "Canvas events"
        // on the calendar grid — wiping only the planner cache leaves zombie pills
        // until the next sync, which never happens after disconnect.
        calendarEventDao.deleteByExternalSource(CANVAS_PLANNER_ITEM_SOURCE)
    }

    companion object {
        private const val TAG = "CanvasSync"
        private const val MAX_PAGES = 10
        private val ISO = DateTimeFormatter.ISO_INSTANT
    }
}
