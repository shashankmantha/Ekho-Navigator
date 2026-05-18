package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.repository.UserCourseRepository
import com.ekhonavigator.core.model.UserCourse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake [UserCourseRepository] for unit tests. Seeded with an initial list,
 * mutated in-memory by write methods; no Firestore round-trip.
 */
class TestUserCourseRepository(
    initial: List<UserCourse> = emptyList(),
) : UserCourseRepository {

    private val coursesFlow = MutableStateFlow(initial)

    val upserts = mutableListOf<UserCourse>()
    val archiveCalls = mutableListOf<Pair<String, Boolean>>()
    val deleteCalls = mutableListOf<String>()

    fun setCourses(courses: List<UserCourse>) {
        coursesFlow.value = courses
    }

    override fun observeCourses(): Flow<List<UserCourse>> = coursesFlow

    override fun observeByFamilyKey(familyKey: String): Flow<UserCourse?> =
        coursesFlow.map { list -> list.firstOrNull { it.familyKey == familyKey } }

    override suspend fun getByFamilyKey(familyKey: String): UserCourse? =
        coursesFlow.value.firstOrNull { it.familyKey == familyKey }

    override suspend fun upsert(course: UserCourse) {
        upserts += course
        coursesFlow.value = coursesFlow.value.filterNot { it.familyKey == course.familyKey } + course
    }

    override suspend fun archive(familyKey: String, archived: Boolean) {
        archiveCalls += familyKey to archived
        coursesFlow.value = coursesFlow.value.map {
            if (it.familyKey == familyKey) it.copy(archived = archived) else it
        }
    }

    override suspend fun delete(familyKey: String) {
        deleteCalls += familyKey
        coursesFlow.value = coursesFlow.value.filterNot { it.familyKey == familyKey }
    }
}
