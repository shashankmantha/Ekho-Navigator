package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.model.UserCourse
import kotlinx.coroutines.flow.Flow

/**
 * Cross-device store of a user's courses. Backed by Firestore, never derived
 * from local Canvas data — survives PAT disconnect, app reinstall, and new
 * devices. Canvas sync seeds new rows additively; it never overwrites colors
 * or archive state the user has set.
 *
 * [observeCourses] emits the empty list while signed out (and again on
 * sign-out mid-session) so downstream combine() chains keep flowing rather
 * than completing.
 */
interface UserCourseRepository {

    fun observeCourses(): Flow<List<UserCourse>>

    fun observeByFamilyKey(familyKey: String): Flow<UserCourse?>

    /** One-shot read — used by lazy-create paths to check existence without subscribing. */
    suspend fun getByFamilyKey(familyKey: String): UserCourse?

    /**
     * Doc id = [UserCourse.familyKey]. Overwrites the row at that key. Callers
     * are responsible for normalizing the family key before passing it in
     * (`CourseColorAssigner.familyKey(normalizeCourseLabel(input))`).
     */
    suspend fun upsert(course: UserCourse)

    /** Soft delete — toggles archived on without dropping the row. */
    suspend fun archive(familyKey: String, archived: Boolean = true)

    /** Hard delete. Use sparingly — once gone, color mapping for old events
     *  tagged with this key is lost. Prefer [archive] in user-facing flows. */
    suspend fun delete(familyKey: String)
}
