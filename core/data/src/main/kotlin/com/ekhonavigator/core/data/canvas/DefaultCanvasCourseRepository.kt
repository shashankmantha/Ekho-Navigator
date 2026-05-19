package com.ekhonavigator.core.data.canvas

import com.ekhonavigator.core.canvas.auth.CanvasAccountSource
import com.ekhonavigator.core.canvas.model.CanvasCourse
import com.ekhonavigator.core.canvas.model.TermNameParser
import com.ekhonavigator.core.canvas.network.CanvasApiProvider
import com.ekhonavigator.core.data.repository.UserCourseRepository
import com.ekhonavigator.core.database.dao.CanvasCourseDao
import com.ekhonavigator.core.database.model.CanvasCourseEntity
import com.ekhonavigator.core.model.CourseColorChoice
import com.ekhonavigator.core.model.UserCourse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// Mirror of CourseColorAssigner.familyKey — duplicated rather than imported
// because core/data shouldn't depend on core/designsystem (which would drag
// Compose into the data layer). Kept in sync by convention.
private val FAMILY_KEY_REGEX = Regex("^([A-Z]+-?\\d+)")
private fun familyKeyOf(code: String): String =
    FAMILY_KEY_REGEX.find(code)?.groupValues?.get(1) ?: code

private const val COURSE_PALETTE_SIZE = 6

@Singleton
internal class DefaultCanvasCourseRepository @Inject constructor(
    private val apiProvider: CanvasApiProvider,
    private val accountSource: CanvasAccountSource,
    private val courseDao: CanvasCourseDao,
    private val userCourseRepository: UserCourseRepository,
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

        enrichUserCourses(entities)
    }

    /**
     * Additive bridge into the user's Firestore course list. Never overwrites
     * an existing row (which would clobber a user-picked color or archive
     * state) — only creates rows for family-keys not yet enrolled. Same family
     * across semesters dedupes naturally because doc-id = familyKey.
     */
    private suspend fun enrichUserCourses(entities: List<CanvasCourseEntity>) {
        // Two reads of the same list: family-key set drives dedup (so a Canvas
        // re-sync doesn't resurrect an archived course); active count drives
        // the slot offset so freshly-added courses start at index 0 when the
        // user has cleared their active set.
        val all = userCourseRepository.observeCourses().first()
        val existingKeys = all.map { it.familyKey }.toSet()
        val activeCount = all.count { !it.archived }

        // Sort by family-key first so new rows land in a deterministic order
        // across devices — both devices see the same "next slot" assignments.
        val needsCreation = entities
            .map { entity -> familyKeyOf(entity.code) to entity }
            .filter { (familyKey, _) -> familyKey !in existingKeys }
            .distinctBy { (familyKey, _) -> familyKey }
            .sortedBy { (familyKey, _) -> familyKey }

        needsCreation.forEachIndexed { index, (familyKey, entity) ->
            userCourseRepository.upsert(
                UserCourse(
                    familyKey = familyKey,
                    code = familyKey,
                    displayName = entity.name.ifBlank { familyKey },
                    colorChoice = CourseColorChoice.Palette((activeCount + index) % COURSE_PALETTE_SIZE),
                    archived = false,
                )
            )
        }
    }

    override suspend fun clearAll() {
        courseDao.deleteAll()
    }
}

object NoCanvasAccountException : Exception("No Canvas account is connected.")
