package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.PlannerItem
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.database.dao.CanvasPlannerItemDao
import com.ekhonavigator.core.database.model.CanvasPlannerItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasPlannerRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val plannerDao: CanvasPlannerItemDao,
) : CanvasPlannerRepository {

    override fun observeItems(start: Instant, end: Instant): Flow<List<PlannerItem>> =
        plannerDao.observeInRange(start, end)
            .map { entities -> entities.map(CanvasPlannerItemEntity::toDomainModel) }

    override suspend fun sync(start: Instant, end: Instant): Result<Unit> = runCatching {
        val api = apiProvider.current() ?: throw NoCanvasAccountException
        val dtos = api.getPlannerItems(
            startDate = ISO.format(start),
            endDate = ISO.format(end),
        )
        val entities = dtos.map { it.toEntity() }
        plannerDao.upsertAll(entities)
        plannerDao.deleteInRangeExcept(start, end, entities.map { it.id })
    }

    companion object {
        private val ISO = DateTimeFormatter.ISO_INSTANT
    }
}
