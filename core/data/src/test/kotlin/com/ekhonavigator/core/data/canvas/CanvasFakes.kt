package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.dao.CanvasPlannerItemDao
import com.ekhonavigator.core.database.model.CalendarEventEntity
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.time.Instant

internal class FakeCanvasApi : CanvasApi {
    var coursesToReturn: List<CanvasCourseDto> = emptyList()
    var plannerItemsToReturn: List<PlannerItemDto> = emptyList()
    var error: Throwable? = null
    var calls = 0
    val plannerCalls = mutableListOf<Pair<String, String>>()
    var lastPlannerContextCodes: List<String>? = null

    override suspend fun getCourses(
        enrollmentState: String,
        perPage: Int,
        include: List<String>,
    ): List<CanvasCourseDto> {
        calls++
        error?.let { throw it }
        return coursesToReturn
    }

    override suspend fun getPlannerItems(
        startDate: String,
        endDate: String,
        contextCodes: List<String>?,
        perPage: Int,
    ): Response<List<PlannerItemDto>> {
        plannerCalls += startDate to endDate
        lastPlannerContextCodes = contextCodes
        error?.let { throw it }
        return Response.success(plannerItemsToReturn)
    }

    // Single-page fake: returns empty for any pagination follow-up. Pagination loop in
    // DefaultCanvasPlannerRepository runs end-to-end against real Canvas in production;
    // the parsing logic itself is unit-tested separately in LinkHeaderTest.
    override suspend fun getPlannerItemsByUrl(url: String): Response<List<PlannerItemDto>> {
        return Response.success(emptyList())
    }
}

internal class FakeCanvasApiProvider(var api: CanvasApi? = null) : CanvasApiProvider {
    override fun current(): CanvasApi? = api
}

internal class FakeCanvasCourseRepository(
    initialCourses: List<CanvasCourse> = emptyList(),
) : CanvasCourseRepository {
    private val coursesFlow = MutableStateFlow(initialCourses)
    var syncCalls = 0
    var nextSyncResult: Result<Unit> = Result.success(Unit)
    var coursesAfterSync: List<CanvasCourse> = initialCourses

    override fun observeCourses(): Flow<List<CanvasCourse>> = coursesFlow

    override suspend fun sync(): Result<Unit> {
        syncCalls++
        coursesFlow.value = coursesAfterSync
        return nextSyncResult
    }
}

internal class FakeCanvasCourseDao : CanvasCourseDao {
    private val state = MutableStateFlow<List<CanvasCourseEntity>>(emptyList())

    fun seed(entities: List<CanvasCourseEntity>) {
        state.value = entities
    }

    fun snapshot(): List<CanvasCourseEntity> = state.value

    override fun observeAll(): Flow<List<CanvasCourseEntity>> = state

    override suspend fun getById(id: String): CanvasCourseEntity? =
        state.value.firstOrNull { it.id == id }

    override suspend fun upsertAll(courses: List<CanvasCourseEntity>) {
        val byId = state.value.associateBy { it.id }.toMutableMap()
        courses.forEach { byId[it.id] = it }
        state.value = byId.values.toList()
    }

    override suspend fun deleteOthers(keepIds: List<String>) {
        state.value = state.value.filter { it.id in keepIds }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}

/**
 * Minimal CalendarEventDao stand-in for tests that exercise Canvas-side writers.
 * Records upserts and prune calls; other methods throw to flag any test that
 * accidentally exercises a path the fake doesn't support — extend in place
 * rather than silently no-op.
 */
internal class FakeCalendarEventDao : CalendarEventDao {
    val upserted = mutableListOf<CalendarEventEntity>()
    val pruneCalls = mutableListOf<PruneCall>()

    data class PruneCall(
        val sourceType: String,
        val rangeStart: Instant,
        val rangeEnd: Instant,
        val keepUids: List<String>,
    )

    override suspend fun upsertEvents(events: List<CalendarEventEntity>) {
        upserted += events
    }

    override suspend fun deleteByExternalSourceInRangeExcept(
        sourceType: String,
        rangeStart: Instant,
        rangeEnd: Instant,
        keepUids: List<String>,
    ) {
        pruneCalls += PruneCall(sourceType, rangeStart, rangeEnd, keepUids)
    }

    override fun observeAllEvents(): Flow<List<CalendarEventEntity>> = unsupported()
    override fun observeBookmarkedEvents(): Flow<List<CalendarEventEntity>> = unsupported()
    override fun observeEventsByDateRange(rangeStart: Instant, rangeEnd: Instant): Flow<List<CalendarEventEntity>> = unsupported()
    override fun observeEventById(id: String): Flow<CalendarEventEntity?> = unsupported()
    override suspend fun getEventById(id: String): CalendarEventEntity? = unsupported()
    override suspend fun updateBookmark(id: String, bookmarked: Boolean) = unsupported()
    override suspend fun upsertEvent(event: CalendarEventEntity) = unsupported()
    override suspend fun deleteICalEventsNotIn(activeUids: List<String>) = unsupported()
    override suspend fun deleteOldEvents(cutoff: Instant) = unsupported()
    override fun observeMyEvents(ownerUid: String): Flow<List<CalendarEventEntity>> = unsupported()
    override fun observeSharedEvents(): Flow<List<CalendarEventEntity>> = unsupported()
    override suspend fun getPendingSyncEvents(): List<CalendarEventEntity> = unsupported()
    override suspend fun updatePendingSync(id: String, pending: Boolean) = unsupported()
    override suspend fun deleteEvent(id: String) = unsupported()
    override suspend fun deleteAllUserEvents() = unsupported()
    override suspend fun clearAllBookmarks() = unsupported()

    private fun unsupported(): Nothing =
        error("FakeCalendarEventDao: extend this fake with the method you need.")
}

internal class FakeCanvasPlannerItemDao : CanvasPlannerItemDao {
    private val state = MutableStateFlow<List<CanvasPlannerItemEntity>>(emptyList())

    fun seed(entities: List<CanvasPlannerItemEntity>) {
        state.value = entities
    }

    fun snapshot(): List<CanvasPlannerItemEntity> = state.value

    override fun observeInRange(start: Instant, end: Instant): Flow<List<CanvasPlannerItemEntity>> =
        state.map { items ->
            items.filter { it.plannableDate >= start && it.plannableDate < end }
                .sortedBy { it.plannableDate }
        }

    override fun observeAll(): Flow<List<CanvasPlannerItemEntity>> = state

    override suspend fun getById(id: String): CanvasPlannerItemEntity? =
        state.value.firstOrNull { it.id == id }

    override suspend fun upsertAll(items: List<CanvasPlannerItemEntity>) {
        val byId = state.value.associateBy { it.id }.toMutableMap()
        items.forEach { byId[it.id] = it }
        state.value = byId.values.toList()
    }

    override suspend fun deleteInRangeExcept(start: Instant, end: Instant, keepIds: List<String>) {
        state.value = state.value.filterNot { item ->
            item.plannableDate >= start && item.plannableDate < end && item.id !in keepIds
        }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }
}
