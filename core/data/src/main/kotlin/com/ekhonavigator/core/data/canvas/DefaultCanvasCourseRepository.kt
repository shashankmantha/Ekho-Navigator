package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasCourseRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val courseDao: CanvasCourseDao,
) : CanvasCourseRepository {

    override fun observeCourses(): Flow<List<CanvasCourse>> =
        courseDao.observeAll().map { entities -> entities.map(CanvasCourseEntity::toDomainModel) }

    override suspend fun sync(): Result<Unit> = runCatching {
        val api = apiProvider.current() ?: throw NoCanvasAccountException
        val dtos = api.getCourses()
        val entities = dtos.map { it.toEntity() }
        courseDao.upsertAll(entities)
        courseDao.deleteOthers(entities.map { it.id })
    }
}

object NoCanvasAccountException : Exception("No Canvas account is connected.")
