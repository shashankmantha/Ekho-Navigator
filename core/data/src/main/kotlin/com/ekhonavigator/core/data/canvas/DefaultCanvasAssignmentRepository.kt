package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.model.CanvasAssignment
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.database.dao.CalendarEventDao
import com.ekhonavigator.core.database.dao.CanvasAssignmentDao
import com.ekhonavigator.core.database.model.CanvasAssignmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasAssignmentRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val assignmentDao: CanvasAssignmentDao,
    @Suppress("unused") private val calendarEventDao: CalendarEventDao,
) : CanvasAssignmentRepository {

    override fun observeForCourse(courseId: String): Flow<List<CanvasAssignment>> =
        assignmentDao.observeForCourse(courseId)
            .map { entities -> entities.map(CanvasAssignmentEntity::toDomainModel) }

    override suspend fun sync(courseId: String): Result<Unit> = runCatching {
        // Implementation lands in A2.3b — pagination + absolutize + description
        // backfill into calendar_events. Skeleton returns success so the
        // ViewModel can wire the trigger now without crashing.
        Unit
    }

    override suspend fun clearAll() {
        assignmentDao.deleteAll()
    }
}
