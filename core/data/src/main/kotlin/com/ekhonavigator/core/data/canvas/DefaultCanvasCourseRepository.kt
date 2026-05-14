package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.model.TermNameParser
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCanvasCourseRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val accountSource: CanvasAccountSource,
    private val courseDao: CanvasCourseDao,
) : CanvasCourseRepository {

    // Filter happens at observe time so the cache keeps the full enrollment
    // record — useful for retrospectives or future term switchers.
    override fun observeCourses(): Flow<List<CanvasCourse>> =
        courseDao.observeAll().map { entities ->
            val now = Instant.now()
            val today = LocalDate.now()
            entities
                .filter { entity ->
                    // Text parse beats term.endAt — endAt is unreliable at CSUCI
                    // (often null or wrong even for past-term courses).
                    val parsed = TermNameParser.parse(entity.termName)
                        ?: TermNameParser.parse(entity.name)
                    val termEndAt = entity.termEndAt
                    when {
                        parsed != null -> parsed.isCurrent(today)
                        termEndAt != null -> termEndAt.isAfter(now)
                        else -> true // No marker anywhere → keep (training / advising)
                    }
                }
                .map(CanvasCourseEntity::toDomainModel)
        }

    override suspend fun sync(): Result<Unit> = runCatching {
        val api = apiProvider.current() ?: throw NoCanvasAccountException
        val dtos = api.getCourses()
        // Absolutize html_url at persist time — Canvas returns it relative,
        // which crashes startActivity if launched as-is.
        val domain = accountSource.currentOrNull()?.domain
        val entities = dtos.map { dto ->
            dto.toEntity().let { entity ->
                val raw = entity.htmlUrl
                if (domain != null && !raw.isNullOrBlank()) {
                    entity.copy(htmlUrl = absolutizeCanvasUrl(raw, domain))
                } else entity
            }
        }
        courseDao.upsertAll(entities)
        courseDao.deleteOthers(entities.map { it.id })
    }

    override suspend fun clearAll() {
        courseDao.deleteAll()
    }
}

object NoCanvasAccountException : Exception("No Canvas account is connected.")
