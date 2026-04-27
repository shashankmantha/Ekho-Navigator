package com.ekhonavigator.core.testing

import com.ekhonavigator.core.data.place.PlaceRepository
import com.ekhonavigator.core.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake [PlaceRepository] for unit tests. Push the visible place list via [emit];
 * collectors observe the change immediately. [resolveFromText] performs a simple
 * case-insensitive substring match — enough for ViewModel-level assertions
 * without dragging in the real word-boundary [com.ekhonavigator.core.data.place.PlaceMatcher].
 */
class TestPlaceRepository : PlaceRepository {

    private val placesFlow = MutableStateFlow<List<Place>>(emptyList())

    fun emit(places: List<Place>) {
        placesFlow.value = places
    }

    override fun observePlaces(): Flow<List<Place>> = placesFlow.asStateFlow()

    override suspend fun getPlace(id: String): Place? =
        placesFlow.value.firstOrNull { it.id == id }

    override suspend fun resolveFromText(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return placesFlow.value.firstOrNull { place ->
            place.name.contains(trimmed, ignoreCase = true) ||
                place.aliases.any { it.contains(trimmed, ignoreCase = true) }
        }?.id
    }
}
