package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.canvas.network.dto.PlannerItemDto
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.dao.CanvasPlannerItemDao
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class FakeCanvasApi : CanvasApi {
    var coursesToReturn: List<CanvasCourseDto> = emptyList()
    var plannerItemsToReturn: List<PlannerItemDto> = emptyList()
    var error: Throwable? = null
    var calls = 0
    val plannerCalls = mutableListOf<Pair<String, String>>()

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
        perPage: Int,
    ): List<PlannerItemDto> {
        plannerCalls += startDate to endDate
        error?.let { throw it }
        return plannerItemsToReturn
    }
}

internal class FakeCanvasApiProvider(var api: CanvasApi? = null) : CanvasApiProvider {
    override fun current(): CanvasApi? = api
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
