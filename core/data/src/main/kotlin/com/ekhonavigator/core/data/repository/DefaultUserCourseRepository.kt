package com.ekhonavigator.core.data.repository

import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.model.CourseColorChoice
import com.ekhonavigator.core.model.UserCourse
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUserCourseRepository @Inject constructor(
    private val authRepository: AuthRepository,
) : UserCourseRepository {

    // Lazy so JVM unit tests can construct the repo without booting FirebaseApp.
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private fun coursesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("courses")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCourses(): Flow<List<UserCourse>> =
        authRepository.userFlow().flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else snapshotsFor(uid)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByFamilyKey(familyKey: String): Flow<UserCourse?> =
        observeCourses().map { courses -> courses.firstOrNull { it.familyKey == familyKey } }

    override suspend fun getByFamilyKey(familyKey: String): UserCourse? {
        val uid = authRepository.getCurrentUserUid() ?: return null
        return runCatching {
            coursesCollection(uid).document(familyKey).get().await().toUserCourseOrNull()
        }.getOrNull()
    }

    override suspend fun upsert(course: UserCourse) {
        val uid = authRepository.getCurrentUserUid() ?: return
        // SetOptions.merge so a partial update from one device doesn't blow
        // away fields written by another (e.g. archive toggled on phone while
        // color changed on tablet).
        coursesCollection(uid).document(course.familyKey)
            .set(course.toFirestoreMap(), SetOptions.merge())
            .await()
    }

    override suspend fun archive(familyKey: String, archived: Boolean) {
        val uid = authRepository.getCurrentUserUid() ?: return
        coursesCollection(uid).document(familyKey)
            .set(mapOf("archived" to archived), SetOptions.merge())
            .await()
    }

    override suspend fun delete(familyKey: String) {
        val uid = authRepository.getCurrentUserUid() ?: return
        coursesCollection(uid).document(familyKey).delete().await()
    }

    private fun snapshotsFor(uid: String): Flow<List<UserCourse>> = callbackFlow {
        val registration = coursesCollection(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.documents.mapNotNull { it.toUserCourseOrNull() })
            }
        }
        awaitClose { registration.remove() }
    }
}

private fun DocumentSnapshot.toUserCourseOrNull(): UserCourse? {
    if (!exists()) return null
    val familyKey = id
    val code = getString("code") ?: familyKey
    val displayName = getString("displayName") ?: code
    val palette = getLong("colorPalette")?.toInt()
    val hex = getString("colorCustomHex")
    val colorChoice: CourseColorChoice = when {
        hex != null -> CourseColorChoice.Custom(hex)
        palette != null -> CourseColorChoice.Palette(palette)
        // No color persisted — assume slot 0 so old docs don't render blank.
        else -> CourseColorChoice.Palette(0)
    }
    val archived = getBoolean("archived") ?: false
    val createdAt = getTimestamp("createdAt")?.toInstant() ?: Instant.now()
    return UserCourse(
        familyKey = familyKey,
        code = code,
        displayName = displayName,
        colorChoice = colorChoice,
        archived = archived,
        createdAt = createdAt,
    )
}

private fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanoseconds.toLong())

private fun UserCourse.toFirestoreMap(): Map<String, Any?> = mapOf(
    "code" to code,
    "displayName" to displayName,
    "colorPalette" to (colorChoice as? CourseColorChoice.Palette)?.slot,
    "colorCustomHex" to (colorChoice as? CourseColorChoice.Custom)?.hex,
    "archived" to archived,
    "createdAt" to Timestamp(createdAt.epochSecond, createdAt.nano),
)
