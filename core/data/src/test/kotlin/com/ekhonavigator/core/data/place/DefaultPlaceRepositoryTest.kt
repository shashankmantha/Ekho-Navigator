package com.ekhonavigator.core.data.place

import android.content.Context
import com.ekhonavigator.core.data.auth.AuthRepository
import com.ekhonavigator.core.data.markers.MarkerRepository
import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.PlaceCategory
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [DefaultPlaceRepository].
 *
 * The repo is a thin in-memory adapter over [PlacesSeed] + [PlaceMatcher],
 * so we test the one behavior with real risk: the seed contains the same
 * Place once per map filter category (e.g. Broome Library shows up under
 * "Library" and "Study"), and [observePlaces] must collapse those to one
 * entry per `id`. Losing that guarantee produces duplicate pins on the map.
 *
 * User markers are merged in via [MarkerRepository] when a user is signed in;
 * the [signedOutAuth] used here keeps the marker stream empty and avoids any
 * Firebase touch — the marker-merge path has its own dedicated tests.
 */
class DefaultPlaceRepositoryTest {

    private val broomeLibrary = Place(
        id = "broome_library",
        name = "Broome Library",
        latitude = 34.160,
        longitude = -119.042,
        category = PlaceCategory.BUILDINGS,
    )

    // Same id as broomeLibrary — represents the seed's duplicate-per-category entry.
    private val broomeLibraryDuplicate = broomeLibrary.copy(category = PlaceCategory.SERVICES)

    private val bellTower = Place(
        id = "bell_tower",
        name = "Bell Tower",
        latitude = 34.161,
        longitude = -119.043,
        category = PlaceCategory.BUILDINGS,
    )

    @Test
    fun `observePlaces collapses seed duplicates to one entry per id`() = runTest {
        val repo = newRepo(seed = listOf(broomeLibrary, broomeLibraryDuplicate, bellTower))

        val places = repo.observePlaces().first()

        assertEquals(2, places.size)
        // distinctBy keeps the first occurrence, so the BUILDINGS-category copy wins.
        assertEquals(PlaceCategory.BUILDINGS, places.first { it.id == "broome_library" }.category)
    }

    @Test
    fun `getPlace returns the place matching the id`() = runTest {
        val repo = newRepo(seed = listOf(broomeLibrary, bellTower))

        assertEquals(broomeLibrary, repo.getPlace("broome_library"))
        assertEquals(bellTower, repo.getPlace("bell_tower"))
    }

    @Test
    fun `getPlace returns null when id is unknown`() = runTest {
        val repo = newRepo(seed = listOf(broomeLibrary))

        assertNull(repo.getPlace("does_not_exist"))
    }

    @Test
    fun `resolveFromText delegates to the matcher against deduped places`() = runTest {
        val repo = newRepo(seed = listOf(broomeLibrary, broomeLibraryDuplicate, bellTower))

        // PlaceMatcher does word-boundary containment against name + aliases.
        assertEquals("broome_library", repo.resolveFromText("Talk at Broome Library tonight"))
        assertEquals("bell_tower", repo.resolveFromText("Meet by Bell Tower"))
        assertNull(repo.resolveFromText("Somewhere off-campus"))
    }

    private fun newRepo(seed: List<Place>): DefaultPlaceRepository = DefaultPlaceRepository(
        seed = { seed },
        matcher = PlaceMatcher(),
        markerRepository = MarkerRepository(),
        authRepository = signedOutAuth,
    )

    private val signedOutAuth = object : AuthRepository {
        override fun getCurrentUserEmail(): String? = null
        override fun getCurrentUserDisplayName(): String? = null
        override fun getCurrentUserUid(): String? = null
        override fun getCurrentUser(): FirebaseUser? = null
        override fun userFlow(): Flow<String?> = flowOf(null)
        override suspend fun signInWithGoogle(context: Context, webClientId: String) = Unit
        override fun signOut() = Unit
    }
}
