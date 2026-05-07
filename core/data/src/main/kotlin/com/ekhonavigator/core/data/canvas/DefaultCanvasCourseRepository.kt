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

    /**
     * Current-term courses only. A course is "current" when its `termEndAt` is in
     * the future, OR when Canvas didn't supply a term end (treated as ongoing —
     * usually training/advising courses without a real semester boundary).
     *
     * Filter happens at observe time, not at sync, so the cache keeps the full
     * enrollment record — useful later for grades retrospectives or term switchers.
     */
    override fun observeCourses(): Flow<List<CanvasCourse>> =
        courseDao.observeAll().map { entities ->
            val now = Instant.now()
            val today = LocalDate.now()
            entities
                .filter { entity ->
                    // Text parse wins over Canvas's term.endAt because endAt is
                    // unreliable at CSUCI: past-term courses commonly have null
                    // (or even future) endAt and Canvas itself shows them as
                    // active. A parseable term name like "Fall 2025" or "FA25"
                    // — read from term.name first, then the course name prefix
                    // — is the strongest signal we have.
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
        // Same absolutize-on-persist treatment as planner items: Canvas returns
        // `html_url` as a relative path that crashes startActivity if launched
        // as-is. Falls back to the relative URL when no account (defensive —
        // the api call above already required one to succeed).
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
