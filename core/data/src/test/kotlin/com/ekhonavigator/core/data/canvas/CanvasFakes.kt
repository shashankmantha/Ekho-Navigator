package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.network.CanvasApi
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.canvas.network.dto.CanvasCourseDto
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeCanvasApi : CanvasApi {
    var coursesToReturn: List<CanvasCourseDto> = emptyList()
    var error: Throwable? = null
    var calls = 0

    override suspend fun getCourses(
        enrollmentState: String,
        perPage: Int,
        include: List<String>,
    ): List<CanvasCourseDto> {
        calls++
        error?.let { throw it }
        return coursesToReturn
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
