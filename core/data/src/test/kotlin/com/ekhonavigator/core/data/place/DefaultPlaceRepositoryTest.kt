package com.ekhonavigator.core.data.place

import com.ekhonavigator.core.model.Place
import com.ekhonavigator.core.model.PlaceCategory
import kotlinx.coroutines.flow.first
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
        val repo = DefaultPlaceRepository(
            seed = { listOf(broomeLibrary, broomeLibraryDuplicate, bellTower) },
            matcher = PlaceMatcher(),
        )

        val places = repo.observePlaces().first()

        assertEquals(2, places.size)
        // distinctBy keeps the first occurrence, so the BUILDINGS-category copy wins.
        assertEquals(PlaceCategory.BUILDINGS, places.first { it.id == "broome_library" }.category)
    }

    @Test
    fun `getPlace returns the place matching the id`() = runTest {
        val repo = DefaultPlaceRepository(
            seed = { listOf(broomeLibrary, bellTower) },
            matcher = PlaceMatcher(),
        )

        assertEquals(broomeLibrary, repo.getPlace("broome_library"))
        assertEquals(bellTower, repo.getPlace("bell_tower"))
    }

    @Test
    fun `getPlace returns null when id is unknown`() = runTest {
        val repo = DefaultPlaceRepository(
            seed = { listOf(broomeLibrary) },
            matcher = PlaceMatcher(),
        )

        assertNull(repo.getPlace("does_not_exist"))
    }

    @Test
    fun `resolveFromText delegates to the matcher against deduped places`() = runTest {
        val repo = DefaultPlaceRepository(
            seed = { listOf(broomeLibrary, broomeLibraryDuplicate, bellTower) },
            matcher = PlaceMatcher(),
        )

        // PlaceMatcher does word-boundary containment against name + aliases.
        assertEquals("broome_library", repo.resolveFromText("Talk at Broome Library tonight"))
        assertEquals("bell_tower", repo.resolveFromText("Meet by Bell Tower"))
        assertNull(repo.resolveFromText("Somewhere off-campus"))
    }
}
